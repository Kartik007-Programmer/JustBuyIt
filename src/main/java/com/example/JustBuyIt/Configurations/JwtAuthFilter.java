package com.example.JustBuyIt.Configurations;

import com.example.JustBuyIt.DTOs.UserPrincipalDto;
import com.example.JustBuyIt.Models.Users;
import com.example.JustBuyIt.Services.JwtService;
import com.example.JustBuyIt.Services.RedisCacheService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Autowired private JwtService jwtService;
    @Autowired private UserDetailsService userDetailsService;
    @Autowired private RedisCacheService redisCacheService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authToken = getAuthTokenFromRequest(request);

        if (authToken != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                String cachedUsername = null;
                try {
                    cachedUsername = redisCacheService.getUserNameFromTokenCache(authToken);
                } catch (Exception redisEx) {
                    logger.warn("Redis token cache lookup failed, falling back to JWT parse: " + redisEx.getMessage());
                }

                String username = (cachedUsername != null)
                        ? cachedUsername
                        : jwtService.getUsernameByToken(authToken);

                if (username != null) {
                    UserPrincipalDto userDto = null;
                    try {
                        userDto = redisCacheService.getUserFromCache(username);
                    } catch (Exception redisEx) {
                        logger.warn("Redis user cache lookup failed, falling back to DB: " + redisEx.getMessage());
                    }

                    if (userDto == null) {
                        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                        if (userDetails instanceof Users u) {
                            userDto = UserPrincipalDto.fromEntity(u);
                            try {
                                redisCacheService.setCacheUser(username, userDto);
                            } catch (Exception ignored) { /* cache write is best-effort */ }
                        }
                    }

                    if (userDto != null && jwtService.isTokenValid(authToken, userDto)) {
                        if (cachedUsername == null) {
                            try { redisCacheService.setCacheToken(authToken, username); }
                            catch (Exception ignored) { /* best-effort */ }
                        }
                        setAuthentication(userDto, request);
                    } else if (userDto != null) {
                        logger.info("JWT rejected for user " + username + " — token invalid or expired");
                    }
                }
            } catch (Exception e) {
                logger.error("Authentication failed: " + e.getMessage(), e);
            }
        }
        filterChain.doFilter(request, response);
    }

    private String getAuthTokenFromRequest(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("JWT_TOKEN".equals(cookie.getName())) return cookie.getValue();
            }
        }
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) return authHeader.substring(7);
        return null;
    }

    private void setAuthentication(UserDetails userDetails, HttpServletRequest request) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}