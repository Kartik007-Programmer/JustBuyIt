package com.example.JustBuyIt.Configurations;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;


@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    JwtAuthFilter jwtAuthFilter;

    @Autowired
    AuthenticationProvider authenticationProvider;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(obj->obj.disable())
                .authorizeHttpRequests(authorizeRequests ->
                        authorizeRequests
                                .requestMatchers("/auth/**","/LoginForm.html","/RegistrationForm.html").permitAll()
                                .requestMatchers("/","/HomePage.html","/OneProduct.html","/css/**").permitAll()
                                .requestMatchers(HttpMethod.GET, "/products/**").permitAll()
                                .requestMatchers(HttpMethod.POST, "/products").hasAuthority("ADMIN")
                                .requestMatchers(HttpMethod.PUT, "/products/**").hasAuthority("ADMIN")
                                .requestMatchers(HttpMethod.DELETE, "/products/**").hasAuthority("ADMIN")
                                .requestMatchers(HttpMethod.POST, "/products/upload", "/multi_products").hasAuthority("ADMIN")
                                .requestMatchers("/users/**")
                                .hasAuthority("USER")
                                .requestMatchers("/admin/**")
                                .hasAuthority("ADMIN")
                                .anyRequest().authenticated())
                .exceptionHandling(exception ->
                        exception.authenticationEntryPoint((request, response, authException) -> {

                            if (isJsonApiRequest(request)) {
                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                response.setContentType("application/json");
                                response.setCharacterEncoding("UTF-8");
                                response.getWriter().write(
                                        "{\"error\":\"UNAUTHORIZED\",\"message\":\"Please log in to continue\"}"
                                );
                            } else {
                                // Any page navigation → browser-style redirect
                                response.sendRedirect("/LoginForm.html");
                            }
                        }))
                .sessionManagement(sessionManagement ->
                        sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private boolean isJsonApiRequest(HttpServletRequest request) {
        // 1. Explicit opt-in header from our JS (most reliable)
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")
                && !accept.contains("text/html")) {
            return true;
        }

        // 2. Legacy XHR header
        String xhr = request.getHeader("X-Requested-With");
        if ("XMLHttpRequest".equalsIgnoreCase(xhr)) {
            return true;
        }

        // 3. Content-Type of a POST/PUT/PATCH with JSON body → treat as API
        String contentType = request.getContentType();
        if (contentType != null && contentType.contains("application/json")) {
            return true;
        }

        // 4. Fallback: no HTML accept header at all + not a GET on a page
        //    (curl / Postman hitting a protected endpoint)
        if (accept != null && !accept.contains("text/html") && !accept.contains("*/*")) {
            return true;
        }

        return false;
    }
}
