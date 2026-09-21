package com.binar.bc.saku_ku.service;

import com.binar.bc.saku_ku.dto.AuthResponseDTO;
import com.binar.bc.saku_ku.dto.CustomerLoginRequest;
import com.binar.bc.saku_ku.dto.CustomerRegisterRequest;
import com.binar.bc.saku_ku.dto.CustomerResponseDTO;
import com.binar.bc.saku_ku.entity.AppCustomerEntity;
import com.binar.bc.saku_ku.entity.CustomerEntity;
import com.binar.bc.saku_ku.exception.BusinessRuleException;
import com.binar.bc.saku_ku.exception.UnauthorizedException;
import com.binar.bc.saku_ku.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for CustomerAuthService — register/login happy + rejection paths.
 * Pure Mockito, no Spring context, no DB/Redis/SMTP.
 */
@ExtendWith(MockitoExtension.class)
class CustomerAuthServiceTest {

    @Mock private CustomerRepository customerRepository;
    @Mock private AppCustomerDetailsService appCustomerDetailsService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private UserPlafondService userPlafondService;
    @Mock private OtpService otpService;
    @Mock private PengajuanService pengajuanService;

    private CustomerAuthService service;

    @BeforeEach
    void setUp() {
        service = new CustomerAuthService(
                customerRepository, appCustomerDetailsService, passwordEncoder,
                jwtService, userPlafondService, otpService, pengajuanService);
    }

    @Nested
    class Register {

        private CustomerRegisterRequest validRequest() {
            CustomerRegisterRequest request = new CustomerRegisterRequest();
            request.setNamaLengkap("Budi Santoso");
            request.setNoHp("081234567890");
            request.setEmail("budi@example.com");
            request.setPassword("Password123!");
            return request;
        }

        @Test
        void happyPath_savesCustomer_assignsPlafond_andSendsOtp() {
            CustomerRegisterRequest request = validRequest();
            when(customerRepository.existsByEmail(request.getEmail())).thenReturn(false);
            when(customerRepository.existsByNoHp(request.getNoHp())).thenReturn(false);
            when(passwordEncoder.encode(request.getPassword())).thenReturn("hashed-password");
            when(customerRepository.save(any(CustomerEntity.class))).thenAnswer(inv -> {
                CustomerEntity entity = inv.getArgument(0);
                if (entity.getId() == null) {
                    entity.setId(UUID.randomUUID());
                }
                return entity;
            });

            CustomerResponseDTO result = service.register(request);

            assertThat(result).isNotNull();
            assertThat(result.getNamaLengkap()).isEqualTo("Budi Santoso");
            assertThat(result.getEmail()).isEqualTo("budi@example.com");
            assertThat(result.getStatus()).isEqualTo("PENDING_VERIFICATION");

            verify(customerRepository, times(2)).save(any(CustomerEntity.class));
            verify(userPlafondService).calculateAndAssign(any(CustomerEntity.class));
            verify(otpService).generateAndSend(eq("budi@example.com"), eq("REGISTER_VERIFY"));
        }

        @Test
        void rejectsDuplicateEmail() {
            CustomerRegisterRequest request = validRequest();
            when(customerRepository.existsByEmail(request.getEmail())).thenReturn(true);

            assertThatThrownBy(() -> service.register(request))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Email sudah terdaftar");

            verify(customerRepository, never()).save(any());
            verify(otpService, never()).generateAndSend(anyString(), anyString());
        }

        @Test
        void rejectsDuplicateNoHp() {
            CustomerRegisterRequest request = validRequest();
            when(customerRepository.existsByEmail(request.getEmail())).thenReturn(false);
            when(customerRepository.existsByNoHp(request.getNoHp())).thenReturn(true);

            assertThatThrownBy(() -> service.register(request))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Nomor HP sudah terdaftar");

            verify(customerRepository, never()).save(any());
        }

        @Test
        void rejectsDuplicateNik_whenNikProvided() {
            CustomerRegisterRequest request = validRequest();
            request.setNik("1234567890123456");
            when(customerRepository.existsByEmail(request.getEmail())).thenReturn(false);
            when(customerRepository.existsByNoHp(request.getNoHp())).thenReturn(false);
            when(customerRepository.existsByNik(request.getNik())).thenReturn(true);

            assertThatThrownBy(() -> service.register(request))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("NIK sudah terdaftar");

            verify(customerRepository, never()).save(any());
        }
    }

    @Nested
    class Login {

        private CustomerLoginRequest loginRequest() {
            CustomerLoginRequest request = new CustomerLoginRequest();
            request.setIdentifier("budi@example.com");
            request.setPassword("Password123!");
            return request;
        }

        private AppCustomerEntity appCustomer(UUID id) {
            AppCustomerEntity appCustomer = new AppCustomerEntity();
            appCustomer.setId(id);
            appCustomer.setUsername("budi@example.com");
            appCustomer.setPassword("hashed-password");
            return appCustomer;
        }

        private CustomerEntity fullCustomer(UUID id, String status, LocalDateTime deletedDate) {
            CustomerEntity customer = new CustomerEntity();
            customer.setId(id);
            customer.setEmail("budi@example.com");
            customer.setStatus(status);
            customer.setDeletedDate(deletedDate);
            return customer;
        }

        @Test
        void happyPath_issuesToken() {
            UUID id = UUID.randomUUID();
            CustomerLoginRequest request = loginRequest();
            AppCustomerEntity principal = appCustomer(id);
            when(appCustomerDetailsService.findCustomer(request.getIdentifier())).thenReturn(Optional.of(principal));
            when(passwordEncoder.matches(request.getPassword(), principal.getPassword())).thenReturn(true);
            when(customerRepository.findById(id)).thenReturn(Optional.of(fullCustomer(id, "ACTIVE", null)));
            when(jwtService.issue(eq("budi@example.com"), eq("CUSTOMER"), any(Instant.class))).thenReturn("jwt-token");

            ResponseEntity<AuthResponseDTO> response = service.login(request);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getToken()).isEqualTo("jwt-token");
        }

        @Test
        void rejectsWrongPassword() {
            CustomerLoginRequest request = loginRequest();
            AppCustomerEntity principal = appCustomer(UUID.randomUUID());
            when(appCustomerDetailsService.findCustomer(request.getIdentifier())).thenReturn(Optional.of(principal));
            when(passwordEncoder.matches(request.getPassword(), principal.getPassword())).thenReturn(false);

            assertThatThrownBy(() -> service.login(request))
                    .isInstanceOf(UnauthorizedException.class);

            verify(jwtService, never()).issue(anyString(), anyString(), any());
        }

        @Test
        void rejectsUnknownIdentifier() {
            CustomerLoginRequest request = loginRequest();
            when(appCustomerDetailsService.findCustomer(request.getIdentifier())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.login(request))
                    .isInstanceOf(UnauthorizedException.class);
        }

        @Test
        void rejectsSoftDeletedAccount_evenWithCorrectPassword() {
            UUID id = UUID.randomUUID();
            CustomerLoginRequest request = loginRequest();
            AppCustomerEntity principal = appCustomer(id);
            when(appCustomerDetailsService.findCustomer(request.getIdentifier())).thenReturn(Optional.of(principal));
            when(passwordEncoder.matches(request.getPassword(), principal.getPassword())).thenReturn(true);
            when(customerRepository.findById(id))
                    .thenReturn(Optional.of(fullCustomer(id, "ACTIVE", LocalDateTime.now())));

            assertThatThrownBy(() -> service.login(request))
                    .isInstanceOf(UnauthorizedException.class);

            verify(jwtService, never()).issue(anyString(), anyString(), any());
        }

        @Test
        void rejectsUnverifiedAccount() {
            UUID id = UUID.randomUUID();
            CustomerLoginRequest request = loginRequest();
            AppCustomerEntity principal = appCustomer(id);
            when(appCustomerDetailsService.findCustomer(request.getIdentifier())).thenReturn(Optional.of(principal));
            when(passwordEncoder.matches(request.getPassword(), principal.getPassword())).thenReturn(true);
            when(customerRepository.findById(id))
                    .thenReturn(Optional.of(fullCustomer(id, "PENDING_VERIFICATION", null)));

            assertThatThrownBy(() -> service.login(request))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("belum diverifikasi");

            verify(jwtService, never()).issue(anyString(), anyString(), any());
        }
    }
}
