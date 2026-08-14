package com.binar.bc.saku_ku.config;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import com.binar.bc.saku_ku.filter.JwtAuthFilter;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Configuration
public class SecurityConfig {
    
    @Bean
    SecurityFilterChain securityFilterChain(
        HttpSecurity http, 
        JwtAuthFilter jwtAuthFilter
    ) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();

        return http
        .csrf(csrfConfigurer -> csrfConfigurer.disable())
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .headers(headersConfigurer -> headersConfigurer
                .referrerPolicy(referrerPolicy -> referrerPolicy.policy(
                ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                        .httpStrictTransportSecurity(hstsConfig -> hstsConfig
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000))
                        .frameOptions(frameOptionsConfig -> frameOptionsConfig.deny()))
                .authorizeHttpRequests(request -> request
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        // .requestMatchers("/api/v1/dashboard/**").hasRole("SUPERADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/user").hasRole("SUPERADMIN")
                        .requestMatchers("/api/v1/user/login").permitAll()
                        .requestMatchers("/api/v1/user/forgot-password").permitAll()
                        .requestMatchers("/api/v1/user/reset-password").permitAll()
                        .anyRequest().authenticated()
                    )
                              .exceptionHandling(handling -> handling
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);

                            Map<String, Object> body = Map.of(
                                    "timestamp", Instant.now().toString(),
                                    "status", HttpStatus.UNAUTHORIZED.value(),
                                    "error", HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                                    "message", "Akses ditolak, Anda tidak terautentikasi"
                            );
                            response.getWriter().write(objectMapper.writeValueAsString(body));
                        })
                )
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean 
    RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.withDefaultRolePrefix()
        .role("SUPERADMIN").implies("MARKETING", "BM", "BACK_OFFICE")
        .build();
    }


    @Value("${app.security.cors-allowed-origins}")
    private List<String> allowedOrigins;

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration konfigurasi = new CorsConfiguration();
        konfigurasi.setAllowedOrigins(allowedOrigins);
        konfigurasi.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        konfigurasi.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        konfigurasi.setAllowCredentials(true);
        konfigurasi.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource sumber = new UrlBasedCorsConfigurationSource();
        sumber.registerCorsConfiguration("/api/**", konfigurasi);
        return sumber;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        String baku = "bcrypt";
        Map<String, PasswordEncoder> encoders = Map.of(baku, new BCryptPasswordEncoder(12));
        return new DelegatingPasswordEncoder(baku, encoders);
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}

