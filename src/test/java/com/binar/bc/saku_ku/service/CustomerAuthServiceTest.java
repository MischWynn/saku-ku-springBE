package com.binar.bc.saku_ku.service;

import com.binar.bc.saku_ku.dto.AuthResponseDTO;
import com.binar.bc.saku_ku.dto.CustomerChangePasswordRequest;
import com.binar.bc.saku_ku.dto.CustomerDeleteAccountRequest;
import com.binar.bc.saku_ku.dto.CustomerForgotPasswordRequest;
import com.binar.bc.saku_ku.dto.CustomerLoginRequest;
import com.binar.bc.saku_ku.dto.CustomerRegisterRequest;
import com.binar.bc.saku_ku.dto.CustomerResetPasswordRequest;
import com.binar.bc.saku_ku.dto.CustomerResponseDTO;
import com.binar.bc.saku_ku.dto.CustomerUpdateRequest;
import com.binar.bc.saku_ku.dto.FcmTokenRequest;
import com.binar.bc.saku_ku.dto.ResendOtpRequest;
import com.binar.bc.saku_ku.dto.VerifyOtpRequest;
import com.binar.bc.saku_ku.entity.AppCustomerEntity;
import com.binar.bc.saku_ku.entity.CustomerEntity;
import com.binar.bc.saku_ku.exception.BusinessRuleException;
import com.binar.bc.saku_ku.exception.UnauthorizedException;
import com.binar.bc.saku_ku.repository.CustomerRepository;

import java.math.BigDecimal;
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
            when(customerRepository.saveAndFlush(any(CustomerEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            CustomerResponseDTO result = service.register(request);

            assertThat(result).isNotNull();
            assertThat(result.getNamaLengkap()).isEqualTo("Budi Santoso");
            assertThat(result.getEmail()).isEqualTo("budi@example.com");
            assertThat(result.getStatus()).isEqualTo("PENDING_VERIFICATION");

            verify(customerRepository).save(any(CustomerEntity.class));
            verify(customerRepository).saveAndFlush(any(CustomerEntity.class));
            verify(userPlafondService).calculateAndAssign(any(CustomerEntity.class));
            verify(otpService).generateAndSend(eq("budi@example.com"), eq("REGISTER_VERIFY"));
        }

        @Test
        void doesNotSendOtp_whenDatabaseRejectsCustomer() {
            // Pelanggaran constraint DB (mis. kolom NOT NULL) harus ketauan SEBELUM email OTP
            // kekirim - jangan sampai orang dapet OTP buat akun yang gak jadi dibuat.
            CustomerRegisterRequest request = validRequest();
            when(customerRepository.existsByEmail(request.getEmail())).thenReturn(false);
            when(customerRepository.existsByNoHp(request.getNoHp())).thenReturn(false);
            when(passwordEncoder.encode(request.getPassword())).thenReturn("hashed-password");
            when(customerRepository.save(any(CustomerEntity.class))).thenAnswer(inv -> inv.getArgument(0));
            when(customerRepository.saveAndFlush(any(CustomerEntity.class)))
                    .thenThrow(new org.springframework.dao.DataIntegrityViolationException("nik NOT NULL"));

            assertThatThrownBy(() -> service.register(request))
                    .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);

            verify(otpService, never()).generateAndSend(any(), any());
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
        void rejectsDuplicateEmail_whenExistingAccountIsActive() {
            CustomerRegisterRequest request = validRequest();
            CustomerEntity active = new CustomerEntity();
            active.setId(UUID.randomUUID());
            active.setStatus("ACTIVE");
            when(customerRepository.existsByEmail(request.getEmail())).thenReturn(true);
            when(customerRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(active));

            assertThatThrownBy(() -> service.register(request))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Email sudah terdaftar");

            verify(otpService, never()).generateAndSend(anyString(), anyString());
        }

        @Test
        void resumesUnfinishedRegistration_insteadOfRejectingEmail() {
            // Registrasi yang ditinggal sebelum OTP: akun yang sama dilanjutin (bukan akun
            // baru), no HP miliknya sendiri gak dianggap bentrok, plafond di-update bukan
            // di-insert ulang, dan OTP baru dikirim.
            CustomerRegisterRequest request = validRequest();
            CustomerEntity unfinished = new CustomerEntity();
            UUID id = UUID.randomUUID();
            unfinished.setId(id);
            unfinished.setStatus("PENDING_VERIFICATION");
            unfinished.setNamaLengkap("Nama Lama");
            when(customerRepository.existsByEmail(request.getEmail())).thenReturn(true);
            when(customerRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(unfinished));
            when(customerRepository.existsByNoHp(request.getNoHp())).thenReturn(true);
            when(customerRepository.findByNoHp(request.getNoHp())).thenReturn(Optional.of(unfinished));
            when(passwordEncoder.encode(request.getPassword())).thenReturn("hashed-password");
            when(customerRepository.save(any(CustomerEntity.class))).thenAnswer(inv -> inv.getArgument(0));
            when(customerRepository.saveAndFlush(any(CustomerEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            CustomerResponseDTO result = service.register(request);

            assertThat(result.getId()).isEqualTo(id);
            assertThat(result.getNamaLengkap()).isEqualTo("Budi Santoso");
            assertThat(result.getStatus()).isEqualTo("PENDING_VERIFICATION");
            verify(userPlafondService).recalculate(unfinished);
            verify(userPlafondService, never()).calculateAndAssign(any());
            verify(otpService).generateAndSend(eq("budi@example.com"), eq("REGISTER_VERIFY"));
        }

        @Test
        void resumingRegistration_stillRejectsNoHpOwnedByAnotherCustomer() {
            CustomerRegisterRequest request = validRequest();
            CustomerEntity unfinished = new CustomerEntity();
            unfinished.setId(UUID.randomUUID());
            unfinished.setStatus("PENDING_VERIFICATION");
            CustomerEntity someoneElse = new CustomerEntity();
            someoneElse.setId(UUID.randomUUID());
            when(customerRepository.existsByEmail(request.getEmail())).thenReturn(true);
            when(customerRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(unfinished));
            when(customerRepository.existsByNoHp(request.getNoHp())).thenReturn(true);
            when(customerRepository.findByNoHp(request.getNoHp())).thenReturn(Optional.of(someoneElse));

            assertThatThrownBy(() -> service.register(request))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Nomor HP sudah terdaftar");

            verify(customerRepository, never()).save(any());
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

    @Nested
    class VerifyRegistrationOtp {

        @Test
        void activatesCustomer_whenOtpValid() {
            VerifyOtpRequest request = new VerifyOtpRequest();
            request.setEmail("budi@example.com");
            request.setCode("123456");
            CustomerEntity customer = new CustomerEntity();
            customer.setEmail("budi@example.com");
            customer.setStatus("PENDING_VERIFICATION");
            when(customerRepository.findByEmail("budi@example.com")).thenReturn(Optional.of(customer));
            when(customerRepository.save(any(CustomerEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            CustomerResponseDTO result = service.verifyRegistrationOtp(request);

            assertThat(result.getStatus()).isEqualTo("ACTIVE");
            verify(otpService).verify("budi@example.com", "REGISTER_VERIFY", "123456");
        }

        @Test
        void throws_whenCustomerNotFound() {
            VerifyOtpRequest request = new VerifyOtpRequest();
            request.setEmail("ghost@example.com");
            when(customerRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.verifyRegistrationOtp(request))
                    .isInstanceOf(BusinessRuleException.class);
        }
    }

    @Nested
    class ResendRegistrationOtp {

        @Test
        void sendsNewOtp_whenNotYetActive() {
            ResendOtpRequest request = new ResendOtpRequest();
            request.setEmail("budi@example.com");
            CustomerEntity customer = new CustomerEntity();
            customer.setEmail("budi@example.com");
            customer.setStatus("PENDING_VERIFICATION");
            when(customerRepository.findByEmail("budi@example.com")).thenReturn(Optional.of(customer));

            service.resendRegistrationOtp(request);

            verify(otpService).generateAndSend("budi@example.com", "REGISTER_VERIFY");
        }

        @Test
        void throws_whenAlreadyActive() {
            ResendOtpRequest request = new ResendOtpRequest();
            request.setEmail("budi@example.com");
            CustomerEntity customer = new CustomerEntity();
            customer.setStatus("ACTIVE");
            when(customerRepository.findByEmail("budi@example.com")).thenReturn(Optional.of(customer));

            assertThatThrownBy(() -> service.resendRegistrationOtp(request))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("sudah terverifikasi");
        }

        @Test
        void throws_whenEmailNotFound() {
            ResendOtpRequest request = new ResendOtpRequest();
            request.setEmail("ghost@example.com");
            when(customerRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.resendRegistrationOtp(request))
                    .isInstanceOf(BusinessRuleException.class);
        }
    }

    @Nested
    class CheckResetPasswordOtp {

        @Test
        void passes_whenOtpValid() {
            VerifyOtpRequest request = new VerifyOtpRequest();
            request.setEmail("budi@example.com");
            request.setCode("111111");
            when(otpService.isValid("budi@example.com", "PASSWORD_RESET", "111111")).thenReturn(true);

            service.checkResetPasswordOtp(request);
        }

        @Test
        void throws_whenOtpInvalid() {
            VerifyOtpRequest request = new VerifyOtpRequest();
            request.setEmail("budi@example.com");
            request.setCode("wrong");
            when(otpService.isValid("budi@example.com", "PASSWORD_RESET", "wrong")).thenReturn(false);

            assertThatThrownBy(() -> service.checkResetPasswordOtp(request))
                    .isInstanceOf(BusinessRuleException.class);
        }
    }

    @Nested
    class ForgotPassword {

        @Test
        void sendsOtp_whenEmailFound() {
            CustomerForgotPasswordRequest request = new CustomerForgotPasswordRequest();
            request.setEmail("budi@example.com");
            when(customerRepository.findByEmail("budi@example.com")).thenReturn(Optional.of(new CustomerEntity()));

            service.forgotPassword(request);

            verify(otpService).generateAndSend("budi@example.com", "PASSWORD_RESET");
        }

        @Test
        void throws_whenEmailNotFound() {
            CustomerForgotPasswordRequest request = new CustomerForgotPasswordRequest();
            request.setEmail("ghost@example.com");
            when(customerRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.forgotPassword(request)).isInstanceOf(BusinessRuleException.class);
        }
    }

    @Nested
    class ResetPassword {

        @Test
        void updatesPasswordHash_whenOtpValid() {
            CustomerResetPasswordRequest request = new CustomerResetPasswordRequest();
            request.setEmail("budi@example.com");
            request.setCode("123456");
            request.setNewPassword("NewPassword123!");
            CustomerEntity customer = new CustomerEntity();
            when(customerRepository.findByEmail("budi@example.com")).thenReturn(Optional.of(customer));
            when(passwordEncoder.encode("NewPassword123!")).thenReturn("new-hash");

            service.resetPassword(request);

            assertThat(customer.getPasswordHash()).isEqualTo("new-hash");
            verify(otpService).verify("budi@example.com", "PASSWORD_RESET", "123456");
        }

        @Test
        void throws_whenCustomerNotFound() {
            CustomerResetPasswordRequest request = new CustomerResetPasswordRequest();
            request.setEmail("ghost@example.com");
            when(customerRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.resetPassword(request)).isInstanceOf(BusinessRuleException.class);
        }
    }

    @Nested
    class ChangePassword {

        @Test
        void updatesPasswordHash_whenOldPasswordCorrect() {
            UUID customerId = UUID.randomUUID();
            CustomerChangePasswordRequest request = new CustomerChangePasswordRequest();
            request.setOldPassword("old");
            request.setNewPassword("new");
            CustomerEntity customer = new CustomerEntity();
            customer.setPasswordHash("old-hash");
            when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
            when(passwordEncoder.matches("old", "old-hash")).thenReturn(true);
            when(passwordEncoder.encode("new")).thenReturn("new-hash");

            service.changePassword(customerId, request);

            assertThat(customer.getPasswordHash()).isEqualTo("new-hash");
        }

        @Test
        void throws_whenOldPasswordWrong() {
            UUID customerId = UUID.randomUUID();
            CustomerChangePasswordRequest request = new CustomerChangePasswordRequest();
            request.setOldPassword("wrong");
            CustomerEntity customer = new CustomerEntity();
            customer.setPasswordHash("old-hash");
            when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
            when(passwordEncoder.matches("wrong", "old-hash")).thenReturn(false);

            assertThatThrownBy(() -> service.changePassword(customerId, request))
                    .isInstanceOf(BusinessRuleException.class);
        }

        @Test
        void throws_whenCustomerNotFound() {
            UUID customerId = UUID.randomUUID();
            when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.changePassword(customerId, new CustomerChangePasswordRequest()))
                    .isInstanceOf(BusinessRuleException.class);
        }
    }

    @Nested
    class GetOwnProfile {

        @Test
        void enrichesWithSisaPlafondAndTier() {
            UUID customerId = UUID.randomUUID();
            CustomerEntity customer = new CustomerEntity();
            customer.setId(customerId);
            when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
            when(pengajuanService.getSisaPlafond(customer)).thenReturn(new BigDecimal("5000000"));
            when(userPlafondService.getTierName(customer)).thenReturn("Silver");

            CustomerResponseDTO result = service.getOwnProfile(customerId);

            assertThat(result.getSisaPlafond()).isEqualByComparingTo("5000000");
            assertThat(result.getTierPlafond()).isEqualTo("Silver");
        }

        @Test
        void throws_whenCustomerNotFound() {
            UUID customerId = UUID.randomUUID();
            when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getOwnProfile(customerId)).isInstanceOf(BusinessRuleException.class);
        }
    }

    @Nested
    class UpdateOwnProfile {

        @Test
        void updatesSimpleFields_andRecalculatesPlafond_whenIncomeChanges() {
            UUID customerId = UUID.randomUUID();
            CustomerEntity customer = new CustomerEntity();
            customer.setId(customerId);
            customer.setEmail("old@mail.com");
            customer.setNoHp("0800000");
            when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
            when(customerRepository.save(any(CustomerEntity.class))).thenAnswer(inv -> inv.getArgument(0));
            when(pengajuanService.getSisaPlafond(any())).thenReturn(BigDecimal.TEN);
            when(userPlafondService.getTierName(any())).thenReturn("Gold");

            CustomerUpdateRequest request = new CustomerUpdateRequest();
            request.setNamaLengkap("Nama Baru");
            request.setPendapatanBulanan(new BigDecimal("6000000"));

            CustomerResponseDTO result = service.updateOwnProfile(customerId, request);

            assertThat(result.getNamaLengkap()).isEqualTo("Nama Baru");
            verify(userPlafondService).recalculate(customer);
        }

        @Test
        void rejectsFotoKtp_whenLocked() {
            UUID customerId = UUID.randomUUID();
            CustomerEntity customer = new CustomerEntity();
            customer.setId(customerId);
            customer.setFotoKtp("foto-lama");
            when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
            when(pengajuanService.getFotoKtpLockReason(customer))
                    .thenReturn("Foto KTP tidak bisa diganti selama pengajuanmu sedang direview.");

            CustomerUpdateRequest request = new CustomerUpdateRequest();
            request.setFotoKtp("foto-baru");

            assertThatThrownBy(() -> service.updateOwnProfile(customerId, request))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("direview");
            assertThat(customer.getFotoKtp()).isEqualTo("foto-lama");
            verify(customerRepository, never()).save(any());
        }

        @Test
        void replacesFotoKtp_whenNotLocked() {
            UUID customerId = UUID.randomUUID();
            CustomerEntity customer = new CustomerEntity();
            customer.setId(customerId);
            customer.setFotoKtp("foto-lama");
            when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
            when(customerRepository.save(any(CustomerEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            CustomerUpdateRequest request = new CustomerUpdateRequest();
            request.setFotoKtp("foto-baru");

            CustomerResponseDTO result = service.updateOwnProfile(customerId, request);

            assertThat(customer.getFotoKtp()).isEqualTo("foto-baru");
            assertThat(result.isHasFotoKtp()).isTrue();
        }

        @Test
        void throws_whenNewEmailAlreadyTaken() {
            UUID customerId = UUID.randomUUID();
            CustomerEntity customer = new CustomerEntity();
            customer.setId(customerId);
            customer.setEmail("old@mail.com");
            when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
            when(customerRepository.existsByEmail("taken@mail.com")).thenReturn(true);

            CustomerUpdateRequest request = new CustomerUpdateRequest();
            request.setEmail("taken@mail.com");

            assertThatThrownBy(() -> service.updateOwnProfile(customerId, request))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Email sudah terdaftar");
        }

        @Test
        void throws_whenNewNoHpAlreadyTaken() {
            UUID customerId = UUID.randomUUID();
            CustomerEntity customer = new CustomerEntity();
            customer.setId(customerId);
            customer.setNoHp("0800000");
            when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
            when(customerRepository.existsByNoHp("0811111")).thenReturn(true);

            CustomerUpdateRequest request = new CustomerUpdateRequest();
            request.setNoHp("0811111");

            assertThatThrownBy(() -> service.updateOwnProfile(customerId, request))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Nomor HP sudah terdaftar");
        }

        @Test
        void throws_whenNikAlreadySetAndRequestTriesToChangeIt() {
            UUID customerId = UUID.randomUUID();
            CustomerEntity customer = new CustomerEntity();
            customer.setId(customerId);
            customer.setNik("1111111111111111");
            when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));

            CustomerUpdateRequest request = new CustomerUpdateRequest();
            request.setNik("2222222222222222");

            assertThatThrownBy(() -> service.updateOwnProfile(customerId, request))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("tidak bisa diubah");
        }

        @Test
        void throws_whenNikAlreadyTakenByAnotherCustomer() {
            UUID customerId = UUID.randomUUID();
            CustomerEntity customer = new CustomerEntity();
            customer.setId(customerId);
            when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
            when(customerRepository.existsByNik("3333333333333333")).thenReturn(true);

            CustomerUpdateRequest request = new CustomerUpdateRequest();
            request.setNik("3333333333333333");

            assertThatThrownBy(() -> service.updateOwnProfile(customerId, request))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("NIK sudah terdaftar");
        }

        @Test
        void throws_whenCustomerNotFound() {
            UUID customerId = UUID.randomUUID();
            when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateOwnProfile(customerId, new CustomerUpdateRequest()))
                    .isInstanceOf(BusinessRuleException.class);
        }
    }

    @Nested
    class DeleteOwnAccount {

        @Test
        void softDeletes_andScramblesUniqueFields_whenPasswordCorrect() {
            UUID customerId = UUID.randomUUID();
            CustomerEntity customer = new CustomerEntity();
            customer.setId(customerId);
            customer.setPasswordHash("hash");
            when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
            when(passwordEncoder.matches("plain", "hash")).thenReturn(true);

            CustomerDeleteAccountRequest request = new CustomerDeleteAccountRequest();
            request.setPassword("plain");

            service.deleteOwnAccount(customerId, request);

            assertThat(customer.getDeletedDate()).isNotNull();
            assertThat(customer.getEmail()).contains("@deleted.sakuku.local");
            verify(customerRepository).save(customer);
        }

        @Test
        void throws_whenPasswordWrong() {
            UUID customerId = UUID.randomUUID();
            CustomerEntity customer = new CustomerEntity();
            customer.setPasswordHash("hash");
            when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
            when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

            CustomerDeleteAccountRequest request = new CustomerDeleteAccountRequest();
            request.setPassword("wrong");

            assertThatThrownBy(() -> service.deleteOwnAccount(customerId, request))
                    .isInstanceOf(BusinessRuleException.class);
        }

        @Test
        void throws_whenAlreadyDeleted() {
            UUID customerId = UUID.randomUUID();
            CustomerEntity customer = new CustomerEntity();
            customer.setPasswordHash("hash");
            customer.setDeletedDate(LocalDateTime.now());
            when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
            when(passwordEncoder.matches("plain", "hash")).thenReturn(true);

            CustomerDeleteAccountRequest request = new CustomerDeleteAccountRequest();
            request.setPassword("plain");

            assertThatThrownBy(() -> service.deleteOwnAccount(customerId, request))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("sudah dihapus");
        }
    }

    @Nested
    class UpdateFcmToken {

        @Test
        void overwritesFcmToken() {
            UUID customerId = UUID.randomUUID();
            CustomerEntity customer = new CustomerEntity();
            when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));

            FcmTokenRequest request = new FcmTokenRequest();
            request.setFcmToken("device-token-abc");

            service.updateFcmToken(customerId, request);

            assertThat(customer.getFcmToken()).isEqualTo("device-token-abc");
            verify(customerRepository).save(customer);
        }

        @Test
        void throws_whenCustomerNotFound() {
            UUID customerId = UUID.randomUUID();
            when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateFcmToken(customerId, new FcmTokenRequest()))
                    .isInstanceOf(BusinessRuleException.class);
        }
    }
}
