package com.binar.bc.saku_ku.service;

import com.binar.bc.saku_ku.dto.PengajuanRequest;
import com.binar.bc.saku_ku.dto.PengajuanReviewRequest;
import com.binar.bc.saku_ku.entity.BungaTenorEntity;
import com.binar.bc.saku_ku.entity.CustomerEntity;
import com.binar.bc.saku_ku.entity.PengajuanEntity;
import com.binar.bc.saku_ku.entity.UserEntity;
import com.binar.bc.saku_ku.entity.UserPlafondEntity;
import com.binar.bc.saku_ku.exception.BusinessRuleException;
import com.binar.bc.saku_ku.repository.BungaTenorRepository;
import com.binar.bc.saku_ku.repository.CustomerRepository;
import com.binar.bc.saku_ku.repository.PengajuanRepository;
import com.binar.bc.saku_ku.repository.UserPlafondRepository;
import com.binar.bc.saku_ku.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for PengajuanService — focused on the cumulative-limit ("sisa plafond") logic
 * (getEffectiveLimit/getSisaPlafond) and the validation inside create() that enforces it.
 * Pure Mockito, no Spring context, no DB.
 */
@ExtendWith(MockitoExtension.class)
class PengajuanServiceTest {

    @Mock private PengajuanRepository pengajuanRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private BungaTenorRepository bungaTenorRepository;
    @Mock private ReviewLogService reviewLogService;
    @Mock private UserRepository userRepository;
    @Mock private NotificationService notificationService;
    @Mock private UserPlafondRepository userPlafondRepository;

    private PengajuanService service;

    @BeforeEach
    void setUp() {
        service = new PengajuanService(
                pengajuanRepository, customerRepository, bungaTenorRepository,
                reviewLogService, userRepository, notificationService, userPlafondRepository);
    }

    private CustomerEntity customerWithFallbackPlafond(BigDecimal plafond) {
        CustomerEntity customer = new CustomerEntity();
        customer.setId(UUID.randomUUID());
        customer.setPlafond(plafond);
        // Profile-completeness fields (pekerjaan/pendapatanBulanan) - create()'s new gate needs
        // both set, so every fixture here defaults to "complete" unless a Create test is
        // specifically exercising that gate (see Create.throwsBusinessRuleException_whenProfile*).
        customer.setPekerjaan("Staff Admin");
        customer.setPendapatanBulanan(new BigDecimal("5000000"));
        // Tanggal lahir juga bagian dari gate "profil lengkap" + minimal 17 tahun (AgePolicy).
        customer.setTanggalLahir(LocalDate.now().minusYears(25));
        customer.setNamaBank("BCA");
        customer.setNomorRekening("1234567890");
        customer.setNamaPemilikRekening("Novita Sari");
        return customer;
    }

    @Nested
    class EffectiveLimit {

        @Test
        void usesUserPlafondEntity_whenPresent() {
            CustomerEntity customer = customerWithFallbackPlafond(new BigDecimal("999"));
            UserPlafondEntity userPlafond = UserPlafondEntity.builder()
                    .limitEfektif(new BigDecimal("12000000"))
                    .build();
            when(userPlafondRepository.findByCustomer_Id(customer.getId())).thenReturn(Optional.of(userPlafond));

            BigDecimal limit = service.getEffectiveLimit(customer);

            assertThat(limit).isEqualByComparingTo("12000000");
        }

        @Test
        void fallsBackToLegacyCustomerPlafondColumn_whenNoUserPlafondEntity() {
            CustomerEntity customer = customerWithFallbackPlafond(new BigDecimal("5000000"));
            when(userPlafondRepository.findByCustomer_Id(customer.getId())).thenReturn(Optional.empty());

            BigDecimal limit = service.getEffectiveLimit(customer);

            assertThat(limit).isEqualByComparingTo("5000000");
        }
    }

    @Nested
    class SisaPlafond {

        @Test
        void returnsNull_whenNoEffectiveLimitAtAll() {
            CustomerEntity customer = customerWithFallbackPlafond(null);
            when(userPlafondRepository.findByCustomer_Id(customer.getId())).thenReturn(Optional.empty());

            assertThat(service.getSisaPlafond(customer)).isNull();
            verify(pengajuanRepository, never()).sumHeldNominalByCustomer(any());
        }

        @Test
        void subtractsHeldNominal_fromEffectiveLimit() {
            CustomerEntity customer = customerWithFallbackPlafond(new BigDecimal("10000000"));
            when(userPlafondRepository.findByCustomer_Id(customer.getId())).thenReturn(Optional.empty());
            when(pengajuanRepository.sumHeldNominalByCustomer(customer.getId())).thenReturn(new BigDecimal("4000000"));

            BigDecimal sisa = service.getSisaPlafond(customer);

            assertThat(sisa).isEqualByComparingTo("6000000");
        }

        @Test
        void neverGoesNegative_whenHeldNominalExceedsLimit() {
            // Defensive clamp — held nominal (e.g. via a race/edge case) shouldn't produce a
            // negative "remaining" figure.
            CustomerEntity customer = customerWithFallbackPlafond(new BigDecimal("10000000"));
            when(userPlafondRepository.findByCustomer_Id(customer.getId())).thenReturn(Optional.empty());
            when(pengajuanRepository.sumHeldNominalByCustomer(customer.getId())).thenReturn(new BigDecimal("15000000"));

            BigDecimal sisa = service.getSisaPlafond(customer);

            assertThat(sisa).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    class Create {

        private final UUID customerId = UUID.randomUUID();
        private final UUID tenorId = UUID.randomUUID();

        private PengajuanRequest request(String nominal) {
            PengajuanRequest req = new PengajuanRequest();
            req.setIdBungaTenor(tenorId);
            req.setNominalPengajuan(new BigDecimal(nominal));
            req.setTujuanPinjaman("MODAL_USAHA");
            return req;
        }

        private BungaTenorEntity activeTenor() {
            BungaTenorEntity tenor = new BungaTenorEntity();
            tenor.setId(tenorId);
            tenor.setTenor(12);
            tenor.setInterestRate(new BigDecimal("3.0"));
            tenor.setStatus("ACTIVE");
            return tenor;
        }

        @Test
        void throwsBusinessRuleException_whenCustomerNotFound() {
            when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.create(customerId, request("1000000")))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Customer tidak ditemukan");
        }

        @Test
        void throwsBusinessRuleException_whenPekerjaanMissing() {
            CustomerEntity customer = customerWithFallbackPlafond(new BigDecimal("10000000"));
            customer.setId(customerId);
            customer.setPekerjaan(null);
            when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));

            assertThatThrownBy(() -> service.create(customerId, request("1000000")))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Lengkapi data pekerjaan dan pendapatan bulanan");

            verify(bungaTenorRepository, never()).findById(any());
        }

        @Test
        void throwsBusinessRuleException_whenPendapatanBulananMissing() {
            CustomerEntity customer = customerWithFallbackPlafond(new BigDecimal("10000000"));
            customer.setId(customerId);
            customer.setPendapatanBulanan(null);
            when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));

            assertThatThrownBy(() -> service.create(customerId, request("1000000")))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Lengkapi data pekerjaan dan pendapatan bulanan");
        }

        @Test
        void throwsBusinessRuleException_whenTanggalLahirMissing() {
            CustomerEntity customer = customerWithFallbackPlafond(new BigDecimal("10000000"));
            customer.setId(customerId);
            customer.setTanggalLahir(null);
            when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));

            assertThatThrownBy(() -> service.create(customerId, request("1000000")))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Lengkapi tanggal lahir");

            verify(bungaTenorRepository, never()).findById(any());
        }

        @Test
        void throwsBusinessRuleException_whenCustomerUnder17() {
            CustomerEntity customer = customerWithFallbackPlafond(new BigDecimal("10000000"));
            customer.setId(customerId);
            customer.setTanggalLahir(LocalDate.now().minusYears(17).plusDays(1));
            when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));

            assertThatThrownBy(() -> service.create(customerId, request("1000000")))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("minimal 17 tahun");

            verify(bungaTenorRepository, never()).findById(any());
        }

        @Test
        void throwsBusinessRuleException_whenRekeningMissing() {
            CustomerEntity customer = customerWithFallbackPlafond(new BigDecimal("10000000"));
            customer.setId(customerId);
            customer.setNomorRekening(null);
            when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));

            assertThatThrownBy(() -> service.create(customerId, request("1000000")))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("rekening bank");

            verify(bungaTenorRepository, never()).findById(any());
        }

        @Test
        void throwsBusinessRuleException_whenBungaTenorNotFound() {
            CustomerEntity customer = customerWithFallbackPlafond(new BigDecimal("10000000"));
            customer.setId(customerId);
            when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
            when(bungaTenorRepository.findById(tenorId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.create(customerId, request("1000000")))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Bunga tenor tidak ditemukan");
        }

        @Test
        void throwsBusinessRuleException_whenBungaTenorInactive() {
            CustomerEntity customer = customerWithFallbackPlafond(new BigDecimal("10000000"));
            customer.setId(customerId);
            BungaTenorEntity tenor = activeTenor();
            tenor.setStatus("INACTIVE");
            when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
            when(bungaTenorRepository.findById(tenorId)).thenReturn(Optional.of(tenor));

            assertThatThrownBy(() -> service.create(customerId, request("1000000")))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("tidak aktif");
        }

        @Test
        void throwsBusinessRuleException_whenNominalExceedsSisaPlafond() {
            CustomerEntity customer = customerWithFallbackPlafond(new BigDecimal("10000000"));
            customer.setId(customerId);
            when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
            when(bungaTenorRepository.findById(tenorId)).thenReturn(Optional.of(activeTenor()));
            when(userPlafondRepository.findByCustomer_Id(customerId)).thenReturn(Optional.empty());
            // Already holding 8jt of the 10jt limit -> only 2jt left, but asking for 5jt.
            when(pengajuanRepository.sumHeldNominalByCustomer(customerId)).thenReturn(new BigDecimal("8000000"));

            assertThatThrownBy(() -> service.create(customerId, request("5000000")))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("melebihi sisa plafond");

            verify(pengajuanRepository, never()).save(any());
        }

        @Test
        void succeeds_whenNominalWithinSisaPlafond() {
            CustomerEntity customer = customerWithFallbackPlafond(new BigDecimal("10000000"));
            customer.setId(customerId);
            when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
            when(bungaTenorRepository.findById(tenorId)).thenReturn(Optional.of(activeTenor()));
            when(userPlafondRepository.findByCustomer_Id(customerId)).thenReturn(Optional.empty());
            when(pengajuanRepository.sumHeldNominalByCustomer(customerId)).thenReturn(new BigDecimal("2000000"));
            when(pengajuanRepository.save(any(PengajuanEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            PengajuanEntity result = service.create(customerId, request("5000000"));

            assertThat(result.getStatus()).isEqualTo("MARKETING_REVIEW");
            assertThat(result.getCustomer()).isEqualTo(customer);
            assertThat(result.getNominalPengajuan()).isEqualByComparingTo("5000000");
            assertThat(result.getTenor()).isEqualTo(12);
            assertThat(result.getInterestRate()).isEqualByComparingTo("3.0");
            assertThat(result.getTujuanPinjaman()).isEqualTo("MODAL_USAHA");
            verify(pengajuanRepository).save(any(PengajuanEntity.class));
        }

        @Test
        void skipsPlafondValidation_whenCustomerHasNoEffectiveLimitAtAll() {
            // Neither a UserPlafondEntity row nor a legacy customer.plafond value -> validation is
            // sidestepped entirely (documented behavior: "kalau tbl_plafond kosong, skip bukan gagal").
            CustomerEntity customer = customerWithFallbackPlafond(null);
            customer.setId(customerId);
            when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
            when(bungaTenorRepository.findById(tenorId)).thenReturn(Optional.of(activeTenor()));
            when(userPlafondRepository.findByCustomer_Id(customerId)).thenReturn(Optional.empty());
            when(pengajuanRepository.save(any(PengajuanEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            PengajuanEntity result = service.create(customerId, request("999999999"));

            assertThat(result).isNotNull();
            verify(pengajuanRepository, never()).sumHeldNominalByCustomer(eq(customerId));
        }
    }

    @Nested
    class Reads {

        @Test
        void getById_returns_whenFound() {
            UUID id = UUID.randomUUID();
            PengajuanEntity pengajuan = new PengajuanEntity();
            pengajuan.setId(id);
            when(pengajuanRepository.findById(id)).thenReturn(Optional.of(pengajuan));

            assertThat(service.getById(id)).isSameAs(pengajuan);
        }

        @Test
        void getById_throws_whenNotFound() {
            UUID id = UUID.randomUUID();
            when(pengajuanRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getById(id)).isInstanceOf(BusinessRuleException.class);
        }

        @Test
        void getByCustomer_delegatesToRepository() {
            UUID customerId = UUID.randomUUID();
            PengajuanEntity pengajuan = new PengajuanEntity();
            when(pengajuanRepository.findByCustomerId(customerId)).thenReturn(List.of(pengajuan));

            assertThat(service.getByCustomer(customerId)).containsExactly(pengajuan);
        }

        @Test
        void getByStatus_delegatesToRepository() {
            PengajuanEntity pengajuan = new PengajuanEntity();
            when(pengajuanRepository.findByStatus("MARKETING_REVIEW")).thenReturn(List.of(pengajuan));

            assertThat(service.getByStatus("MARKETING_REVIEW")).containsExactly(pengajuan);
        }

        @Test
        void getAll_delegatesToRepository() {
            PengajuanEntity pengajuan = new PengajuanEntity();
            when(pengajuanRepository.findAll()).thenReturn(List.of(pengajuan));

            assertThat(service.getAll()).containsExactly(pengajuan);
        }
    }

    @Nested
    class ReviewWorkflow {

        private final UUID id = UUID.randomUUID();

        private PengajuanEntity pengajuanWithStatus(String status) {
            PengajuanEntity pengajuan = new PengajuanEntity();
            pengajuan.setId(id);
            pengajuan.setStatus(status);
            pengajuan.setNominalPengajuan(new BigDecimal("5000000"));
            pengajuan.setCustomer(new CustomerEntity());
            return pengajuan;
        }

        private UserEntity staffUser() {
            UserEntity user = new UserEntity();
            user.setUsername("dewi.marketing");
            return user;
        }

        @BeforeEach
        void stubSaveAndUser() {
            org.mockito.Mockito.lenient().when(pengajuanRepository.save(any(PengajuanEntity.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            org.mockito.Mockito.lenient().when(userRepository.findByUsernameAndDeletedDateIsNull("dewi.marketing"))
                    .thenReturn(Optional.of(staffUser()));
        }

        @Test
        void marketingApprove_movesToBmReview() {
            when(pengajuanRepository.findById(id)).thenReturn(Optional.of(pengajuanWithStatus("MARKETING_REVIEW")));

            PengajuanEntity result = service.marketingApprove(id, "dewi.marketing", new PengajuanReviewRequest());

            assertThat(result.getStatus()).isEqualTo("BM_REVIEW");
            verify(reviewLogService).record(any(), any(), eq("APPROVE"), eq("MARKETING_REVIEW"), eq("BM_REVIEW"), any());
        }

        @Test
        void marketingApprove_throws_whenWrongStatus() {
            when(pengajuanRepository.findById(id)).thenReturn(Optional.of(pengajuanWithStatus("BM_REVIEW")));

            assertThatThrownBy(() -> service.marketingApprove(id, "dewi.marketing", new PengajuanReviewRequest()))
                    .isInstanceOf(BusinessRuleException.class);
        }

        @Test
        void marketingReject_movesToMarketingRejected() {
            when(pengajuanRepository.findById(id)).thenReturn(Optional.of(pengajuanWithStatus("MARKETING_REVIEW")));

            PengajuanEntity result = service.marketingReject(id, "dewi.marketing", new PengajuanReviewRequest());

            assertThat(result.getStatus()).isEqualTo("MARKETING_REJECTED");
        }

        @Test
        void bmApprove_setsNominalDisetujui_andMovesToBackofficeReview() {
            when(pengajuanRepository.findById(id)).thenReturn(Optional.of(pengajuanWithStatus("BM_REVIEW")));
            PengajuanReviewRequest request = new PengajuanReviewRequest();
            request.setNominalDisetujui(new BigDecimal("4000000"));

            PengajuanEntity result = service.bmApprove(id, "dewi.marketing", request);

            assertThat(result.getStatus()).isEqualTo("BACKOFFICE_REVIEW");
            assertThat(result.getNominalDisetujui()).isEqualByComparingTo("4000000");
        }

        @Test
        void bmApprove_throws_whenNominalDisetujuiMissing() {
            when(pengajuanRepository.findById(id)).thenReturn(Optional.of(pengajuanWithStatus("BM_REVIEW")));

            assertThatThrownBy(() -> service.bmApprove(id, "dewi.marketing", new PengajuanReviewRequest()))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("wajib diisi");
        }

        @Test
        void bmApprove_throws_whenNominalDisetujuiExceedsRequested() {
            when(pengajuanRepository.findById(id)).thenReturn(Optional.of(pengajuanWithStatus("BM_REVIEW")));
            PengajuanReviewRequest request = new PengajuanReviewRequest();
            request.setNominalDisetujui(new BigDecimal("9999999999"));

            assertThatThrownBy(() -> service.bmApprove(id, "dewi.marketing", request))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("tidak boleh lebih besar");
        }

        @Test
        void bmReject_movesToBmRejected() {
            when(pengajuanRepository.findById(id)).thenReturn(Optional.of(pengajuanWithStatus("BM_REVIEW")));

            PengajuanEntity result = service.bmReject(id, "dewi.marketing", new PengajuanReviewRequest());

            assertThat(result.getStatus()).isEqualTo("BM_REJECTED");
        }

        @Test
        void disburse_movesToDisbursed_andSetsTanggalPencairan() {
            when(pengajuanRepository.findById(id)).thenReturn(Optional.of(pengajuanWithStatus("BACKOFFICE_REVIEW")));

            PengajuanEntity result = service.disburse(id, "dewi.marketing");

            assertThat(result.getStatus()).isEqualTo("DISBURSED");
            assertThat(result.getTanggalPencairan()).isNotNull();
        }

        @Test
        void disburse_throws_whenStaffUserNotFound() {
            when(pengajuanRepository.findById(id)).thenReturn(Optional.of(pengajuanWithStatus("BACKOFFICE_REVIEW")));
            when(userRepository.findByUsernameAndDeletedDateIsNull("ghost")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.disburse(id, "ghost")).isInstanceOf(BusinessRuleException.class);
        }
    }

    @Nested
    class Cancel {

        private final UUID id = UUID.randomUUID();

        @BeforeEach
        void stubSave() {
            org.mockito.Mockito.lenient().when(pengajuanRepository.save(any(PengajuanEntity.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
        }

        @Test
        void cancelByCustomer_cancels_whenOwnedAndCancellableStatus() {
            UUID customerId = UUID.randomUUID();
            CustomerEntity customer = new CustomerEntity();
            customer.setId(customerId);
            PengajuanEntity pengajuan = new PengajuanEntity();
            pengajuan.setId(id);
            pengajuan.setStatus("MARKETING_REVIEW");
            pengajuan.setCustomer(customer);
            when(pengajuanRepository.findById(id)).thenReturn(Optional.of(pengajuan));

            PengajuanEntity result = service.cancelByCustomer(id, customerId);

            assertThat(result.getStatus()).isEqualTo("CANCELLED");
        }

        @Test
        void cancelByCustomer_throws_whenNotOwner() {
            CustomerEntity customer = new CustomerEntity();
            customer.setId(UUID.randomUUID());
            PengajuanEntity pengajuan = new PengajuanEntity();
            pengajuan.setId(id);
            pengajuan.setCustomer(customer);
            when(pengajuanRepository.findById(id)).thenReturn(Optional.of(pengajuan));

            assertThatThrownBy(() -> service.cancelByCustomer(id, UUID.randomUUID()))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("tidak berhak");
        }

        @Test
        void cancelByCustomer_throws_whenNotCancellableStatus() {
            UUID customerId = UUID.randomUUID();
            CustomerEntity customer = new CustomerEntity();
            customer.setId(customerId);
            PengajuanEntity pengajuan = new PengajuanEntity();
            pengajuan.setId(id);
            pengajuan.setStatus("DISBURSED");
            pengajuan.setCustomer(customer);
            when(pengajuanRepository.findById(id)).thenReturn(Optional.of(pengajuan));

            assertThatThrownBy(() -> service.cancelByCustomer(id, customerId))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("tidak bisa dibatalkan");
        }

        @Test
        void cancelBySuperadmin_cancels_whenNotTerminal() {
            PengajuanEntity pengajuan = new PengajuanEntity();
            pengajuan.setId(id);
            pengajuan.setStatus("BM_REVIEW");
            when(pengajuanRepository.findById(id)).thenReturn(Optional.of(pengajuan));

            PengajuanEntity result = service.cancelBySuperadmin(id);

            assertThat(result.getStatus()).isEqualTo("CANCELLED");
        }

        @Test
        void cancelBySuperadmin_throws_whenAlreadyDisbursed() {
            PengajuanEntity pengajuan = new PengajuanEntity();
            pengajuan.setId(id);
            pengajuan.setStatus("DISBURSED");
            when(pengajuanRepository.findById(id)).thenReturn(Optional.of(pengajuan));

            assertThatThrownBy(() -> service.cancelBySuperadmin(id))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("sudah dicairkan");
        }

        @Test
        void cancelBySuperadmin_throws_whenAlreadyTerminal() {
            PengajuanEntity pengajuan = new PengajuanEntity();
            pengajuan.setId(id);
            pengajuan.setStatus("CANCELLED");
            when(pengajuanRepository.findById(id)).thenReturn(Optional.of(pengajuan));

            assertThatThrownBy(() -> service.cancelBySuperadmin(id)).isInstanceOf(BusinessRuleException.class);
        }
    }
}
