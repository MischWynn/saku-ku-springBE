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
                        .requestMatchers("/api/v1/user/login").permitAll()
                        .requestMatchers("/api/v1/user/forgot-password").permitAll()
                        .requestMatchers("/api/v1/user/reset-password").permitAll()
                        .requestMatchers("/api/v1/customer/register").permitAll()
                        .requestMatchers("/api/v1/customer/login").permitAll()

                        // rule spesifik /me HARUS di atas rule wildcard {id}
                        .requestMatchers(org.springframework.http.HttpMethod.PATCH, "/api/v1/user/me")
                                .hasAnyRole("SUPERADMIN", "MARKETING", "BM", "BACK_OFFICE")
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/user/me")
                                .hasAnyRole("SUPERADMIN", "MARKETING", "BM", "BACK_OFFICE")

                        //rule spesifik untuk /tenor 
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/bunga-tenor/**")
                                .hasAnyRole("SUPERADMIN", "MARKETING")
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/bunga-tenor")
                                .hasRole("SUPERADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.PATCH, "/api/v1/bunga-tenor/{id}")
                                .hasRole("SUPERADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/api/v1/bunga-tenor/{id}")
                                .hasRole("SUPERADMIN")

                        // === Pengajuan: Customer ===
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/pengajuan")
                                .hasRole("CUSTOMER")
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/pengajuan/me")
                                .hasRole("CUSTOMER")
                        .requestMatchers(org.springframework.http.HttpMethod.PATCH, "/api/v1/pengajuan/{id}/cancel")
                                .hasRole("CUSTOMER")

                        // === Pengajuan: MARKETING actions ===
                        .requestMatchers(org.springframework.http.HttpMethod.PATCH, "/api/v1/pengajuan/{id}/marketing-approve")
                                .hasRole("MARKETING")
                        .requestMatchers(org.springframework.http.HttpMethod.PATCH, "/api/v1/pengajuan/{id}/marketing-reject")
                                .hasRole("MARKETING")

                        // === Pengajuan: BM actions ===
                        .requestMatchers(org.springframework.http.HttpMethod.PATCH, "/api/v1/pengajuan/{id}/bm-approve")
                                .hasRole("BM")
                        .requestMatchers(org.springframework.http.HttpMethod.PATCH, "/api/v1/pengajuan/{id}/bm-reject")
                                .hasRole("BM")

                        // === Pengajuan: BACK_OFFICE action ===
                        .requestMatchers(org.springframework.http.HttpMethod.PATCH, "/api/v1/pengajuan/{id}/disburse")
                                .hasRole("BACK_OFFICE")

                        // === Pengajuan: SUPERADMIN override ===
                        .requestMatchers(org.springframework.http.HttpMethod.PATCH, "/api/v1/pengajuan/{id}/cancel-admin")
                                .hasRole("SUPERADMIN")

                        // === Pengajuan: staff read (semua role staff boleh liat) ===
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/pengajuan/**")
                                .hasAnyRole("SUPERADMIN", "MARKETING", "BM", "BACK_OFFICE")

                        // baru rule wildcard {id}, taruh di bawah
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/user").hasRole("SUPERADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/user").hasRole("SUPERADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.PATCH, "/api/v1/user/{id}").hasRole("SUPERADMIN")

                        .anyRequest().authenticated()
                )                              .exceptionHandling(handling -> handling
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
                                             .accessDeniedHandler((request, response, accessDeniedException) -> {
                        response.setStatus(HttpStatus.FORBIDDEN.value());
                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                        Map<String, Object> body = Map.of(
                                "timestamp", Instant.now().toString(),
                                "status", HttpStatus.FORBIDDEN.value(),
                                "error", HttpStatus.FORBIDDEN.getReasonPhrase(),
                                "message", "Akses ditolak, Anda tidak memiliki hak akses"
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

