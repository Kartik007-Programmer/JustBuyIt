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

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private RedisCacheService redisCacheService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String authToken = getAuthTokenFromRequest(request);

        if (authToken != null && SecurityContextHolder.getContext().getAuthentication() == null){
            try {
                String CachedUsername = redisCacheService.getUserNameFromTokenCache(authToken);
                String username = (CachedUsername != null) ? CachedUsername : jwtService.getUsernameByToken(authToken);

                if (username != null) {
                    UserPrincipalDto userDto = redisCacheService.getUserFromCache(username);
                    if (userDto == null) {
                        UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
                        if (userDetails instanceof Users) {
                            userDto = UserPrincipalDto.fromEntity((Users) userDetails);
                            redisCacheService.setCacheUser(username, userDto);
                        }
                    }

                    if (userDto != null && jwtService.isTokenValid(authToken, userDto)) {
                        if (CachedUsername == null) {
                            redisCacheService.setCacheToken(authToken, username);
                        }
                        setAuthenticationByUserDetails(userDto, request);
                    }
                }
            } catch (Exception e) {
                logger.error("Authentication failed: " + e.getMessage());
            }
        }
        filterChain.doFilter(request, response);
    }

    private String getAuthTokenFromRequest(HttpServletRequest request) {
        if (request.getCookies() != null){
            for (Cookie cookie : request.getCookies()) {
                if ("JWT_TOKEN".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    private void setAuthenticationByUserDetails(UserDetails userDetails, HttpServletRequest request) {
        UsernamePasswordAuthenticationToken usernamePasswordAuthToken = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities());

        usernamePasswordAuthToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthToken);
    }

}
