package com.yumai.config;

import com.yumai.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Stateless JWT security with role-based access control (FR-01.3).
 * Route-level rules follow the SRS use-case/role matrix (SRS 4.2);
 * finer rules are applied with @PreAuthorize in controllers.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Value("${yumai.cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public PasswordEncoder passwordEncoder() {
        // NFR-05: BCrypt for all stored passwords
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .headers(h -> h.frameOptions(f -> f.sameOrigin())) // allow H2 console in dev
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/login", "/api/auth/register").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                // UC-02 / FR-01.4: user management is Admin-only (Manager may read)
                .requestMatchers(HttpMethod.GET, "/api/users/**").hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers("/api/users/**").hasRole("ADMIN")
                // UC-03: menu management by Admin/Manager, read for all roles
                .requestMatchers(HttpMethod.GET, "/api/menu/**").authenticated()
                .requestMatchers("/api/menu/**").hasAnyRole("ADMIN", "MANAGER")
                // FR-02.5 / UC-06: billing history restricted to Manager/Admin
                .requestMatchers("/api/bills/**").hasAnyRole("ADMIN", "MANAGER")
                // UC-04/05: orders by Staff and above
                .requestMatchers("/api/orders/**").hasAnyRole("ADMIN", "MANAGER", "STAFF")
                // FR-03.3: stock edits Manager/Admin; reads + waste logs Staff and above
                .requestMatchers(HttpMethod.GET, "/api/inventory/**").hasAnyRole("ADMIN", "MANAGER", "STAFF")
                .requestMatchers("/api/inventory/waste-logs").hasAnyRole("ADMIN", "MANAGER", "STAFF")
                .requestMatchers("/api/inventory/**").hasAnyRole("ADMIN", "MANAGER")
                // UC-10: dashboard for all authenticated roles
                .requestMatchers("/api/dashboard/**").authenticated()
                // UC-13/14/15 + FR-06.4: AI and reports for Manager/Admin
                .requestMatchers("/api/ai/**").hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers("/api/reports/**").hasAnyRole("ADMIN", "MANAGER")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Content-Disposition"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
