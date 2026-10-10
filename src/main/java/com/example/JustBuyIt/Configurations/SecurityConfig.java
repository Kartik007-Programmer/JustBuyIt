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
                                .requestMatchers("/auth/**","/LoginForm.html","/RegistrationForm.html","/ForgotPassword.html","/ResetPassword.html").permitAll()
                                .requestMatchers("/","/HomePage.html","/OneProduct.html","/css/**","/js/**","/Icons/**").permitAll()
                                .requestMatchers(HttpMethod.GET,    "/products/**").permitAll()

                                // Product writes: ADMIN + SECONDARY_ADMIN (fine-grained checks live in the service)
                                .requestMatchers(HttpMethod.POST,   "/products", "/products/upload").hasAnyAuthority("ADMIN", "SECONDARY_ADMIN")
                                .requestMatchers(HttpMethod.PUT,    "/products/**").hasAnyAuthority("ADMIN", "SECONDARY_ADMIN")
                                .requestMatchers(HttpMethod.DELETE, "/products/**").hasAnyAuthority("ADMIN", "SECONDARY_ADMIN")

                                // Batch upload: both admin roles allowed.
                                // The 3-product cap for SECONDARY_ADMIN is enforced in SecurityService.assertCanBatchCreateProducts.
                                .requestMatchers(HttpMethod.POST, "/multi_products").hasAnyAuthority("ADMIN", "SECONDARY_ADMIN")

                                // Admin area (ban/unban, list admins) stays ADMIN-only
                                .requestMatchers("/admin/all", "/admin/users/*/ban", "/admin/users/*/unban").hasAuthority("ADMIN")

                                // Admin area (/admin Frontend) stays for both ADMIN and SECONDARY_ADMIN
                                .requestMatchers("/Admin_Pages/**","/admin","/admin/users").hasAnyAuthority("ADMIN","SECONDARY_ADMIN")

                                // Regular user endpoints and users frontend
                                .requestMatchers("/users/**","/Users_Pages/**").hasAuthority("USER")
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
