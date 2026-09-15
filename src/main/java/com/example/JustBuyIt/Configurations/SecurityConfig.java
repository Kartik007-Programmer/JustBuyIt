package com.example.JustBuyIt.Configurations;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
                                .requestMatchers("/HomePage.html","/OneProduct.html")
                                .hasAuthority("USER")
                                .requestMatchers("/UpdateProduct.html","/AdminDashboad.html")
                                .hasAuthority("ADMIN")
                                .requestMatchers("/AdminOneProduct.html","/ViewAllProduct.html","/AddProduct.html")
                                .hasAuthority("ADMIN")
                                .anyRequest().authenticated())
                .exceptionHandling(exception->
                        exception.authenticationEntryPoint((request, response, authException) -> {
                            response.sendRedirect("/LoginForm.html");
                        }))
                .sessionManagement(sessionManagement ->
                        sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
