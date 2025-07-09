package com.epam.config;

import com.epam.edai.run8.team15.exception.CustomAccessDeniedHandler;
import com.epam.edai.run8.team15.filter.JwtFilter;
import com.epam.edai.run8.team15.service.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@RequiredArgsConstructor
@Configuration
@EnableWebSecurity
public class SpringSecurityConfig {

    private final UserDetailsServiceImpl userDetailsService;
    private final JwtFilter jwtFilter;

    private static final String[] WAITER_ENDPOINTS = {
            "/waiter/**",
            "/bookings/waiter/**"
    };

    private static final String[] CUSTOMER_ENDPOINTS = {
            "/bookings/client/**",
            "/reservations/**",
            "/feedbacks",
            "/feedbacks/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        return http.authorizeHttpRequests(request -> request
                        .requestMatchers(WAITER_ENDPOINTS).hasRole("WAITER")
                        .requestMatchers(CUSTOMER_ENDPOINTS).hasRole("CUSTOMER")
                        .anyRequest().permitAll())
                .csrf(AbstractHttpConfigurer::disable)
                .exceptionHandling(exception -> exception
                        .accessDeniedHandler(customAccessDeniedHandler())
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService).passwordEncoder(new BCryptPasswordEncoder());
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration auth) throws Exception {
        return auth.getAuthenticationManager();
    }

    @Bean
    public CustomAccessDeniedHandler customAccessDeniedHandler() {
        return new CustomAccessDeniedHandler();
    }
}
