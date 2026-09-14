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
                        .requestMatchers("/api/v1/customer/verify-otp").permitAll()
                        .requestMatchers("/api/v1/customer/resend-otp").permitAll()
                        .requestMatchers("/api/v1/customer/forgot-password").permitAll()
                        .requestMatchers("/api/v1/customer/reset-password").permitAll()

                        // rule spesifik /me HARUS di atas anyRequest().authenticated() — tanpa ini,
                        // GET/PATCH /customer/me kena rule generik yang nerima role manapun (termasuk
                        // staff), padahal @AuthenticationPrincipal AppCustomerEntity bakal null/gagal
                        // kalau yang lagi login itu staff, bukan customer.
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/customer/me")
                                .hasRole("CUSTOMER")
                        .requestMatchers(org.springframework.http.HttpMethod.PATCH, "/api/v1/customer/me")
                                .hasRole("CUSTOMER")

                        // "/me" HARUS di atas wildcard "/pengajuan/**" di bawah - kebalik sebelumnya
                        // (13 Sept), bikin customer selalu 403 pas GET pengajuan/me karena wildcard
                        // staff-only ke-match duluan. Pola yang sama (spesifik sebelum wildcard) udah
                        // bener diterapin di /user/me dan /role-menu/me, cuma di sini kebalik.
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/pengajuan/me")
                                .hasRole("CUSTOMER")
                        // Timeline versi customer (DTO terfilter, endpoint terpisah dari
                        // /{id}/history yang staff pakai) - taro di atas wildcard, pelajaran sama.
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/pengajuan/{id}/history/me")
                                .hasRole("CUSTOMER")
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/pengajuan/**")
        .hasAnyRole("SUPERADMIN", "MARKETING", "BM", "BACK_OFFICE")

                        // rule spesifik /me dan /change-password HARUS di atas rule wildcard {id}
                        .requestMatchers(org.springframework.http.HttpMethod.PATCH, "/api/v1/user/me")
                                .hasAnyRole("SUPERADMIN", "MARKETING", "BM", "BACK_OFFICE")
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/user/me")
                                .hasAnyRole("SUPERADMIN", "MARKETING", "BM", "BACK_OFFICE")
                        .requestMatchers(org.springframework.http.HttpMethod.PATCH, "/api/v1/user/change-password")
                                .hasAnyRole("SUPERADMIN", "MARKETING", "BM", "BACK_OFFICE")

                        // Riwayat Review Saya — staff liat riwayat review-nya sendiri (superadmin gak pernah nge-log review, jadi gak dikasih akses)
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/review-log/me")
                                .hasAnyRole("MARKETING", "BM", "BACK_OFFICE")

                        // Publicly browsable (butuh diliat tanpa login — homepage/simulasi customer,
                        // dan Android buat isi tenor picker pas Ajukan Pinjaman). Write ops tetap
                        // dibatasin di bawah (POST/PATCH/DELETE superadmin-only).
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/bunga-tenor/**")
                                .permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/bunga-tenor")
                                .hasRole("SUPERADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.PATCH, "/api/v1/bunga-tenor/{id}")
                                .hasRole("SUPERADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/api/v1/bunga-tenor/{id}")
                                .hasRole("SUPERADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/dashboard/superadmin/**")
                                .hasRole("SUPERADMIN")

                        // === Pengajuan: Customer ===
                        // (GET /pengajuan/me udah dideklarasiin di atas, sebelum wildcard /pengajuan/**)
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/pengajuan")
                                .hasRole("CUSTOMER")
                        .requestMatchers(org.springframework.http.HttpMethod.PATCH, "/api/v1/pengajuan/{id}/cancel")
                                .hasRole("CUSTOMER")
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/notifikasi/**")
                                .hasRole("CUSTOMER")
                        .requestMatchers(org.springframework.http.HttpMethod.PATCH, "/api/v1/notifikasi/{id}/read")
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
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/user/{id}").hasRole("SUPERADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.PATCH, "/api/v1/user/{id}").hasRole("SUPERADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/api/v1/user/{id}").hasRole("SUPERADMIN")

                        // === Master Role (superadmin only) ===
                        .requestMatchers("/api/v1/role/**").hasRole("SUPERADMIN")

                        // === Master Menu (superadmin only) ===
                        .requestMatchers("/api/v1/menu/**").hasRole("SUPERADMIN")

                        // Publicly browsable tier catalog — sama alasan kayak bunga-tenor di atas.
                        // Rule spesifik GET ini HARUS di atas wildcard superadmin-only di bawahnya.
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/plafond/**")
                                .permitAll()

                        // === Master Plafond writes (superadmin only) ===
                        .requestMatchers("/api/v1/plafond/**").hasRole("SUPERADMIN")

                        // rule spesifik /me HARUS di atas rule wildcard /api/v1/role-menu/** —
                        // dipakai sidebar buat semua role staff, bukan cuma superadmin.
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/role-menu/me")
                                .hasAnyRole("SUPERADMIN", "MARKETING", "BM", "BACK_OFFICE")

                        // === Master Access (superadmin only) ===
                        .requestMatchers("/api/v1/role-menu/**").hasRole("SUPERADMIN")

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
        konfigurasi.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
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

