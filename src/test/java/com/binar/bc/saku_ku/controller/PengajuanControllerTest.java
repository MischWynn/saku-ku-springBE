package com.binar.bc.saku_ku.controller;

import com.binar.bc.saku_ku.entity.AppCustomerEntity;
import com.binar.bc.saku_ku.entity.CustomerEntity;
import com.binar.bc.saku_ku.entity.PengajuanEntity;
import com.binar.bc.saku_ku.exception.BusinessRuleException;
import com.binar.bc.saku_ku.filter.JwtAuthFilter;
import com.binar.bc.saku_ku.service.PengajuanService;
import com.binar.bc.saku_ku.service.ReviewLogService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller-slice tests for PengajuanController — request validation, HTTP status codes,
 * and the ApiResponse<T> JSON shape. Service layer is fully mocked (MockitoBean); no real
 * DB, and JwtAuthFilter excluded from the slice (same reasoning as CustomerAuthControllerTest).
 */
@WebMvcTest(
        controllers = PengajuanController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthFilter.class)
)
@AutoConfigureMockMvc(addFilters = false)
class PengajuanControllerTest {

    // See CustomerAuthControllerTest for why this is needed: @WebMvcTest alone doesn't register
    // AuthenticationPrincipalArgumentResolver.
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
    private PengajuanService pengajuanService;

    @MockitoBean
    private ReviewLogService reviewLogService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private AppCustomerEntity customerPrincipal(UUID id) {
        AppCustomerEntity principal = new AppCustomerEntity();
        principal.setId(id);
        principal.setUsername("budi@example.com");
        return principal;
    }

    private void authenticateAsCustomer(UUID id) {
        AppCustomerEntity principal = customerPrincipal(id);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private PengajuanEntity samplePengajuan(UUID customerId, String status) {
        PengajuanEntity pengajuan = new PengajuanEntity();
        pengajuan.setId(UUID.randomUUID());
        CustomerEntity customer = new CustomerEntity();
        customer.setId(customerId);
        pengajuan.setCustomer(customer);
        pengajuan.setNominalPengajuan(new BigDecimal("5000000"));
        pengajuan.setStatus(status);
        return pengajuan;
    }

    @Test
    void create_validPayload_returns200WithApiResponseShape() throws Exception {
        UUID customerId = UUID.randomUUID();
        authenticateAsCustomer(customerId);
        PengajuanEntity saved = samplePengajuan(customerId, "MARKETING_REVIEW");
        when(pengajuanService.create(eq(customerId), any())).thenReturn(saved);

        String payload = """
                {
                  "idBungaTenor": "%s",
                  "nominalPengajuan": 5000000,
                  "tujuanPinjaman": "MODAL_USAHA"
                }
                """.formatted(UUID.randomUUID());

        mockMvc.perform(post("/api/v1/pengajuan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
                        .principal(new UsernamePasswordAuthenticationToken(
                                customerPrincipal(customerId), null, customerPrincipal(customerId).getAuthorities())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.status").value("MARKETING_REVIEW"));
    }

    @Test
    void create_zeroNominal_returns400_andNeverCallsService() throws Exception {
        UUID customerId = UUID.randomUUID();
        authenticateAsCustomer(customerId);

        String payload = """
                {
                  "idBungaTenor": "%s",
                  "nominalPengajuan": 0
                }
                """.formatted(UUID.randomUUID());

        mockMvc.perform(post("/api/v1/pengajuan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
                        .principal(new UsernamePasswordAuthenticationToken(
                                customerPrincipal(customerId), null, customerPrincipal(customerId).getAuthorities())))
                .andExpect(status().isBadRequest());

        verify(pengajuanService, never()).create(any(), any());
    }

    @Test
    void create_missingBungaTenor_returns400() throws Exception {
        UUID customerId = UUID.randomUUID();

        String payload = """
                {
                  "nominalPengajuan": 5000000
                }
                """;

        mockMvc.perform(post("/api/v1/pengajuan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
                        .principal(new UsernamePasswordAuthenticationToken(
                                customerPrincipal(customerId), null, customerPrincipal(customerId).getAuthorities())))
                .andExpect(status().isBadRequest());

        verify(pengajuanService, never()).create(any(), any());
    }

    @Test
    void create_serviceRejectsOverLimit_returns422() throws Exception {
        UUID customerId = UUID.randomUUID();
        authenticateAsCustomer(customerId);
        when(pengajuanService.create(eq(customerId), any()))
                .thenThrow(new BusinessRuleException("Nominal pengajuan melebihi sisa plafond Anda"));

        String payload = """
                {
                  "idBungaTenor": "%s",
                  "nominalPengajuan": 999999999
                }
                """.formatted(UUID.randomUUID());

        mockMvc.perform(post("/api/v1/pengajuan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
                        .principal(new UsernamePasswordAuthenticationToken(
                                customerPrincipal(customerId), null, customerPrincipal(customerId).getAuthorities())))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.message").value("Nominal pengajuan melebihi sisa plafond Anda"));
    }

    @Test
    void getByStatus_returns200WithListPayload() throws Exception {
        when(pengajuanService.getByStatus("MARKETING_REVIEW"))
                .thenReturn(List.of(samplePengajuan(UUID.randomUUID(), "MARKETING_REVIEW")));

        mockMvc.perform(get("/api/v1/pengajuan/status/MARKETING_REVIEW"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].status").value("MARKETING_REVIEW"));
    }

    @Test
    void bmApprove_missingNominalDisetujui_returns422WithBackendMessage() throws Exception {
        UUID pengajuanId = UUID.randomUUID();
        when(pengajuanService.bmApprove(eq(pengajuanId), anyString(), any()))
                .thenThrow(new BusinessRuleException("Nominal disetujui wajib diisi"));

        mockMvc.perform(patch("/api/v1/pengajuan/{id}/bm-approve", pengajuanId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .principal(new UsernamePasswordAuthenticationToken("bm.staff", null, List.of())))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.message").value("Nominal disetujui wajib diisi"));
    }

    @Test
    void cancelByCustomer_notOwner_returns422() throws Exception {
        UUID pengajuanId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        authenticateAsCustomer(customerId);
        when(pengajuanService.cancelByCustomer(eq(pengajuanId), eq(customerId)))
                .thenThrow(new BusinessRuleException("Anda tidak berhak membatalkan pengajuan ini"));

        mockMvc.perform(patch("/api/v1/pengajuan/{id}/cancel", pengajuanId)
                        .principal(new UsernamePasswordAuthenticationToken(
                                customerPrincipal(customerId), null, customerPrincipal(customerId).getAuthorities())))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.message").value("Anda tidak berhak membatalkan pengajuan ini"));
    }
}
