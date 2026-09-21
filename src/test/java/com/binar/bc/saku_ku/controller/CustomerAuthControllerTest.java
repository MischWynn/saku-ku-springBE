package com.binar.bc.saku_ku.controller;

import com.binar.bc.saku_ku.dto.AuthResponseDTO;
import com.binar.bc.saku_ku.dto.CustomerResponseDTO;
import com.binar.bc.saku_ku.entity.AppCustomerEntity;
import com.binar.bc.saku_ku.exception.UnauthorizedException;
import com.binar.bc.saku_ku.filter.JwtAuthFilter;
import com.binar.bc.saku_ku.service.CustomerAuthService;
import com.binar.bc.saku_ku.service.TokenBlacklistService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller-slice tests for CustomerAuthController — request validation, HTTP status codes,
 * and the ApiResponse<T> JSON shape. Service layer is fully mocked (MockitoBean); no real
 * DB/Redis/SMTP, and JwtAuthFilter is excluded from the sliced context since its own
 * dependencies (JwtService, AppUserDetailsService, ...) aren't part of a @WebMvcTest slice.
 */
@WebMvcTest(
        controllers = CustomerAuthController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthFilter.class)
)
@AutoConfigureMockMvc(addFilters = false)
class CustomerAuthControllerTest {

    // @WebMvcTest doesn't import Boot's SecurityAutoConfiguration, so @AuthenticationPrincipal has
    // no resolver registered by default (it silently falls through to a model-attribute binder
    // instead of reading SecurityContextHolder). Registering just the resolver here wires up
    // @AuthenticationPrincipal without needing a full SecurityFilterChain/JwtAuthFilter.
    @TestConfiguration
    static class AuthenticationPrincipalTestConfig implements WebMvcConfigurer {
        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new AuthenticationPrincipalArgumentResolver());
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomerAuthService customerAuthService;

    // Not exercised by any test below, but needed to satisfy CustomerAuthController's
    // constructor now that it also depends on TokenBlacklistService (added for /logout).
    @MockitoBean
    private TokenBlacklistService tokenBlacklistService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private String validRegisterPayload() {
        return """
                {
                  "namaLengkap": "Budi Santoso",
                  "noHp": "081234567890",
                  "email": "budi@example.com",
                  "password": "Password123!"
                }
                """;
    }

    @Test
    void register_validPayload_returns200WithApiResponseShape() throws Exception {
        CustomerResponseDTO dto = new CustomerResponseDTO();
        dto.setId(UUID.randomUUID());
        dto.setNamaLengkap("Budi Santoso");
        dto.setEmail("budi@example.com");
        dto.setStatus("PENDING_VERIFICATION");
        when(customerAuthService.register(any())).thenReturn(dto);

        mockMvc.perform(post("/api/v1/customer/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRegisterPayload()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.namaLengkap").value("Budi Santoso"))
                .andExpect(jsonPath("$.data.email").value("budi@example.com"))
                .andExpect(jsonPath("$.data.status").value("PENDING_VERIFICATION"));
    }

    @Test
    void register_blankNamaLengkap_returns400_andNeverCallsService() throws Exception {
        String invalidPayload = """
                {
                  "namaLengkap": "",
                  "noHp": "081234567890",
                  "email": "budi@example.com",
                  "password": "Password123!"
                }
                """;

        mockMvc.perform(post("/api/v1/customer/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest());

        verify(customerAuthService, never()).register(any());
    }

    @Test
    void register_invalidEmailFormat_returns400() throws Exception {
        String invalidPayload = """
                {
                  "namaLengkap": "Budi Santoso",
                  "noHp": "081234567890",
                  "email": "not-an-email",
                  "password": "Password123!"
                }
                """;

        mockMvc.perform(post("/api/v1/customer/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest());

        verify(customerAuthService, never()).register(any());
    }

    @Test
    void login_validCredentials_returns200WithToken() throws Exception {
        when(customerAuthService.login(any())).thenReturn(ResponseEntity.ok(new AuthResponseDTO("jwt-token")));

        String payload = """
                {
                  "identifier": "budi@example.com",
                  "password": "Password123!"
                }
                """;

        mockMvc.perform(post("/api/v1/customer/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.type").value("Bearer"));
    }

    @Test
    void login_wrongCredentials_returns401() throws Exception {
        when(customerAuthService.login(any())).thenThrow(new UnauthorizedException("Email/No HP atau password salah"));

        String payload = """
                {
                  "identifier": "budi@example.com",
                  "password": "wrong-password"
                }
                """;

        mockMvc.perform(post("/api/v1/customer/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Email/No HP atau password salah"));
    }

    @Test
    void login_blankPassword_returns400() throws Exception {
        String payload = """
                {
                  "identifier": "budi@example.com",
                  "password": ""
                }
                """;

        mockMvc.perform(post("/api/v1/customer/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());

        verify(customerAuthService, never()).login(any());
    }

    @Test
    void getOwnProfile_withAuthenticatedCustomerPrincipal_returns200() throws Exception {
        UUID customerId = UUID.randomUUID();
        AppCustomerEntity principal = new AppCustomerEntity();
        principal.setId(customerId);
        principal.setUsername("budi@example.com");

        CustomerResponseDTO dto = new CustomerResponseDTO();
        dto.setId(customerId);
        dto.setNamaLengkap("Budi Santoso");
        dto.setSisaPlafond(new BigDecimal("5000000"));
        when(customerAuthService.getOwnProfile(customerId)).thenReturn(dto);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        mockMvc.perform(get("/api/v1/customer/me")
                        .principal(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.namaLengkap").value("Budi Santoso"))
                .andExpect(jsonPath("$.data.sisaPlafond").value(5000000));
    }
}
