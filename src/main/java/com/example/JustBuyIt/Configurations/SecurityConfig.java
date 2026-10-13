package com.example.JustBuyIt.Configurations;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    JwtAuthFilter jwtAuthFilter;

    @Autowired
    AuthenticationProvider authenticationProvider;

    @Value("${app.base-url:http://localhost:8080}")
    private String appBaseUrl;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // Public assets + auth pages
                        .requestMatchers(
                                "/auth/**",
                                "/LoginForm.html", "/RegistrationForm.html",
                                "/ForgotPassword.html", "/ResetPassword.html",
                                "/", "/HomePage.html", "/OneProduct.html",
                                "/Users_Pages/**",
                                "/css/**", "/js/**", "/Icons/**"
                        ).permitAll()

                        // Public read-only product APIs
                        .requestMatchers(HttpMethod.GET, "/products/**").permitAll()

                        // Product writes (fine-grained checks live in the service)
                        .requestMatchers(HttpMethod.POST,   "/products", "/products/upload").hasAnyAuthority("ADMIN", "SECONDARY_ADMIN")
                        .requestMatchers(HttpMethod.PUT,    "/products/**").hasAnyAuthority("ADMIN", "SECONDARY_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/products/**").hasAnyAuthority("ADMIN", "SECONDARY_ADMIN")
                        .requestMatchers(HttpMethod.POST,   "/multi_products").hasAnyAuthority("ADMIN", "SECONDARY_ADMIN")

                        // Admin-only management
                        .requestMatchers("/admin/all", "/admin/users/*/ban", "/admin/users/*/unban").hasAuthority("ADMIN")

                        // Admin area
                        .requestMatchers("/Admin_Pages/**", "/admin", "/admin/users").hasAnyAuthority("ADMIN", "SECONDARY_ADMIN")

                        // User API (this is what the user pages call)
                        .requestMatchers("/users/**").hasAuthority("USER")

                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex.authenticationEntryPoint((request, response, authException) -> {
                    if (isJsonApiRequest(request)) {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType("application/json");
                        response.setCharacterEncoding("UTF-8");
                        response.getWriter().write(
                                "{\"error\":\"UNAUTHORIZED\",\"message\":\"Please log in to continue\"}"
                        );
                    } else {
                        String target = request.getRequestURI();
                        String qs = request.getQueryString();
                        if (qs != null && !qs.isEmpty()) target += "?" + qs;
                        String encoded = java.net.URLEncoder.encode(target, java.nio.charset.StandardCharsets.UTF_8);
                        response.sendRedirect("/LoginForm.html?redirect=" + encoded);
                    }
                }))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        // Exact origin — "*" is illegal with allowCredentials(true)
        cfg.setAllowedOrigins(List.of(
                appBaseUrl,
                "http://localhost:8080",
                "http://127.0.0.1:8080"
        ));
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setExposedHeaders(List.of("Set-Cookie"));
        cfg.setAllowCredentials(true);
        cfg.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", cfg);
        return src;
    }

    private boolean isJsonApiRequest(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json") && !accept.contains("text/html")) return true;

        String xhr = request.getHeader("X-Requested-With");
        if ("XMLHttpRequest".equalsIgnoreCase(xhr)) return true;

        String contentType = request.getContentType();
        if (contentType != null && contentType.contains("application/json")) return true;

        if (accept != null && !accept.contains("text/html") && !accept.contains("*/*")) return true;

        return false;
    }
}