package com.epam.filter;

import com.epam.edai.run8.team15.utils.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
@Component
public class JwtFilter extends OncePerRequestFilter{
    private final UserDetailsService userDetailsService;

    private final JwtUtil jwtUtil;

//    @Override
//    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
//        String authorizationHeader = request.getHeader("Authorization");
//        String username = null;
//        String jwt = null;
//        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
//            jwt = authorizationHeader.substring(7);
//            username = jwtUtil.extractUsername(jwt);
//        }
//        if (username != null) {
//            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
//            if (jwtUtil.validateToken(jwt)) {
//                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
//                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
//                SecurityContextHolder.getContext().setAuthentication(auth);
//            }
//        }
//        chain.doFilter(request, response);
//    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        log.info("Starting JWT authentication filter...");

        String authorizationHeader = request.getHeader("Authorization");
        String username = null;
        String jwt = null;

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            log.info("Authorization header found, extracting JWT...");
            jwt = authorizationHeader.substring(7);
            try {
                username = jwtUtil.extractUsername(jwt);
                log.info("Extracted username from JWT: {}", username);
            } catch (Exception e) {
                log.error("Error extracting username from token", e);
            }
        } else {
            log.warn("Authorization header missing or does not start with 'Bearer '");
        }

        if (username != null) {
            log.info("Loading user details for username: {}", username);
            try {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                log.info("UserDetails loaded. Authorities: {}", userDetails.getAuthorities());

                if (jwtUtil.validateToken(jwt)) {
                    log.info("Token is valid. Setting authentication in context.");
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                } else {
                    log.warn("Token validation failed for user: {}", username);
                }
            } catch (Exception e) {
                log.error("Error loading user details or validating token", e);
            }
        } else {
            log.warn("Username is null, skipping authentication setup.");
        }

        log.info("Continuing filter chain...");
        chain.doFilter(request, response);
    }
}