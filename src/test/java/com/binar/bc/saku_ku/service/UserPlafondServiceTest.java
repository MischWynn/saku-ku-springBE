package com.binar.bc.saku_ku.service;

import com.binar.bc.saku_ku.entity.CustomerEntity;
import com.binar.bc.saku_ku.entity.PlafondEntity;
import com.binar.bc.saku_ku.entity.UserPlafondEntity;
import com.binar.bc.saku_ku.repository.PlafondRepository;
import com.binar.bc.saku_ku.repository.UserPlafondRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the credit-limit ("plafond") formula in UserPlafondService.
 * Pure Mockito, no Spring context — exercises computeRawAmount()/employmentMultiplier()/pickTier()
 * indirectly via the public calculateAndAssign()/recalculate() methods.
 */
@ExtendWith(MockitoExtension.class)
class UserPlafondServiceTest {

    @Mock
    private PlafondRepository plafondRepository;

    @Mock
    private UserPlafondRepository userPlafondRepository;

    private UserPlafondService service;

    private static final BigDecimal MINIMUM_PLAFOND = new BigDecimal("2000000");

    @BeforeEach
    void setUp() {
        service = new UserPlafondService(plafondRepository, userPlafondRepository);
        // save() just echoes back whatever entity it was given, like a real repository would.
        // lenient() because the empty-tier-catalog tests never reach save() at all.
        lenient().when(userPlafondRepository.save(any(UserPlafondEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private PlafondEntity tier(String nama, String limitMaksimal) {
        return PlafondEntity.builder()
                .idPlafond(UUID.randomUUID())
                .namaPlafond(nama)
                .limitMaksimal(new BigDecimal(limitMaksimal))
                .status("ACTIVE")
                .build();
    }

    private List<PlafondEntity> defaultTierCatalog() {
        // Ascending order, as PlafondRepository.findByStatusOrderByLimitMaksimalAsc would return.
        return List.of(
                tier("Bronze", "5000000"),
                tier("Silver", "10000000"),
                tier("Gold", "25000000"),
                tier("Platinum", "50000000")
        );
    }

    private CustomerEntity customer(BigDecimal income, BigDecimal debt, String tipePekerjaan) {
        CustomerEntity customer = new CustomerEntity();
        customer.setId(UUID.randomUUID());
        customer.setPendapatanBulanan(income);
        customer.setUtangBerjalan(debt);
        customer.setTipePekerjaan(tipePekerjaan);
        return customer;
    }

    @Nested
    class TierCatalogEmpty {

        @Test
        void calculateAndAssign_returnsNull_whenNoActiveTiers() {
            when(plafondRepository.findByStatusOrderByLimitMaksimalAsc("ACTIVE")).thenReturn(List.of());
            CustomerEntity customer = customer(new BigDecimal("5000000"), BigDecimal.ZERO, "SWASTA");

            UserPlafondEntity result = service.calculateAndAssign(customer);

            assertThat(result).isNull();
            verify(userPlafondRepository, never()).save(any());
        }

        @Test
        void recalculate_returnsNull_whenNoActiveTiers() {
            when(plafondRepository.findByStatusOrderByLimitMaksimalAsc("ACTIVE")).thenReturn(List.of());
            CustomerEntity customer = customer(new BigDecimal("5000000"), BigDecimal.ZERO, "SWASTA");

            UserPlafondEntity result = service.recalculate(customer);

            assertThat(result).isNull();
            verify(userPlafondRepository, never()).save(any());
        }
    }

    @Nested
    class FormulaAndTierSelection {

        @BeforeEach
        void stubTiers() {
            when(plafondRepository.findByStatusOrderByLimitMaksimalAsc("ACTIVE"))
                    .thenReturn(defaultTierCatalog());
        }

        @Test
        void swasta_stableIncome_usesFullMultiplier_andPicksGoldNotItsCeiling() {
            // (5jt - 1jt) * 3 * 1.0 = 12jt raw. Smallest tier whose ceiling >= 12jt is Gold (25jt),
            // but the ACTUAL assigned limit must be the raw 12jt, not Gold's 25jt ceiling.
            CustomerEntity customer = customer(new BigDecimal("5000000"), new BigDecimal("1000000"), "SWASTA");

            UserPlafondEntity result = service.calculateAndAssign(customer);

            assertThat(result.getPlafond().getNamaPlafond()).isEqualTo("Gold");
            assertThat(result.getLimitEfektif()).isEqualByComparingTo("12000000");
            assertThat(result.getLimitEfektif()).isNotEqualByComparingTo(result.getPlafond().getLimitMaksimal());
            // Sync back onto the customer entity too.
            assertThat(customer.getPlafond()).isEqualByComparingTo("12000000");
        }

        @Test
        void asnTniPolri_andBumnBumd_getSameMultiplierAsSwasta() {
            CustomerEntity asn = customer(new BigDecimal("5000000"), new BigDecimal("1000000"), "ASN_TNI_POLRI");
            CustomerEntity bumn = customer(new BigDecimal("5000000"), new BigDecimal("1000000"), "BUMN_BUMD");

            assertThat(service.calculateAndAssign(asn).getLimitEfektif()).isEqualByComparingTo("12000000");
            assertThat(service.calculateAndAssign(bumn).getLimitEfektif()).isEqualByComparingTo("12000000");
        }

        @Test
        void wiraswasta_getsLowerMultiplier_andPicksSilverTier() {
            // (5jt - 1jt) * 3 * 0.8 = 9.6jt raw -> fits under Silver's 10jt ceiling.
            CustomerEntity customer = customer(new BigDecimal("5000000"), new BigDecimal("1000000"), "WIRASWASTA");

            UserPlafondEntity result = service.calculateAndAssign(customer);

            assertThat(result.getPlafond().getNamaPlafond()).isEqualTo("Silver");
            assertThat(result.getLimitEfektif()).isEqualByComparingTo("9600000");
        }

        @Test
        void nonProfit_matchesWiraswastaMultiplier() {
            CustomerEntity customer = customer(new BigDecimal("5000000"), new BigDecimal("1000000"), "NON_PROFIT");

            UserPlafondEntity result = service.calculateAndAssign(customer);

            assertThat(result.getLimitEfektif()).isEqualByComparingTo("9600000");
        }

        @Test
        void freelance_isLowerThanWiraswasta() {
            // (5jt - 1jt) * 3 * 0.6 = 7.2jt — explicitly lower than wiraswasta's 9.6jt for the
            // same income/debt, per the documented ordering (freelance is less stable).
            CustomerEntity customer = customer(new BigDecimal("5000000"), new BigDecimal("1000000"), "FREELANCE");

            UserPlafondEntity result = service.calculateAndAssign(customer);

            assertThat(result.getLimitEfektif()).isEqualByComparingTo("7200000");
        }

        @Test
        void tidakBekerja_usesDefensiveFloorMultiplier_whenIncomeStillPositive() {
            // Positive net income despite TIDAK_BEKERJA (e.g. passive income) — floor multiplier 0.3
            // still applies rather than skipping straight to the absolute minimum.
            // (3jt - 0) * 3 * 0.3 = 2.7jt.
            CustomerEntity customer = customer(new BigDecimal("3000000"), BigDecimal.ZERO, "TIDAK_BEKERJA");

            UserPlafondEntity result = service.calculateAndAssign(customer);

            assertThat(result.getLimitEfektif()).isEqualByComparingTo("2700000");
        }

        @Test
        void legacyKaryawanAndPns_stillSupported_sameAsSwasta() {
            CustomerEntity karyawan = customer(new BigDecimal("5000000"), new BigDecimal("1000000"), "KARYAWAN");
            CustomerEntity pns = customer(new BigDecimal("5000000"), new BigDecimal("1000000"), "PNS");

            assertThat(service.calculateAndAssign(karyawan).getLimitEfektif()).isEqualByComparingTo("12000000");
            assertThat(service.calculateAndAssign(pns).getLimitEfektif()).isEqualByComparingTo("12000000");
        }

        @Test
        void unknownOrNullTipePekerjaan_fallsBackToPointSevenMultiplier() {
            // (5jt - 1jt) * 3 * 0.7 = 8.4jt
            CustomerEntity nullType = customer(new BigDecimal("5000000"), new BigDecimal("1000000"), null);
            CustomerEntity unknownType = customer(new BigDecimal("5000000"), new BigDecimal("1000000"), "SOMETHING_ELSE");

            assertThat(service.calculateAndAssign(nullType).getLimitEfektif()).isEqualByComparingTo("8400000");
            assertThat(service.calculateAndAssign(unknownType).getLimitEfektif()).isEqualByComparingTo("8400000");
        }

        @Test
        void nullIncome_fallsBackToMinimumPlafond_andPicksSmallestTier() {
            CustomerEntity customer = customer(null, null, "SWASTA");

            UserPlafondEntity result = service.calculateAndAssign(customer);

            assertThat(result.getPlafond().getNamaPlafond()).isEqualTo("Bronze");
            assertThat(result.getLimitEfektif()).isEqualByComparingTo(MINIMUM_PLAFOND);
        }

        @Test
        void debtExceedingIncome_fallsBackToMinimumPlafond() {
            // netIncome <= 0 -> raw floored to the absolute minimum before any multiplier applies.
            CustomerEntity customer = customer(new BigDecimal("3000000"), new BigDecimal("5000000"), "SWASTA");

            UserPlafondEntity result = service.calculateAndAssign(customer);

            assertThat(result.getLimitEfektif()).isEqualByComparingTo(MINIMUM_PLAFOND);
        }

        @Test
        void computedAmountAboveHighestTier_clampsToThatTiersCeiling() {
            // (100jt - 0) * 3 * 1.0 = 300jt raw, above even Platinum's 50jt ceiling -> pickTier()
            // falls through to the last (highest) tier, and THIS is the one case where limitEfektif
            // legitimately equals the tier ceiling (because it's clamped down, not derived raw).
            CustomerEntity customer = customer(new BigDecimal("100000000"), BigDecimal.ZERO, "SWASTA");

            UserPlafondEntity result = service.calculateAndAssign(customer);

            assertThat(result.getPlafond().getNamaPlafond()).isEqualTo("Platinum");
            assertThat(result.getLimitEfektif()).isEqualByComparingTo("50000000");
        }

        @Test
        void limitEfektif_neverGoesBelowMinimumPlafond_evenIfRawIsSmallerThanMinimum() {
            // A tiny but still-positive raw amount must still be floored up to the Rp2,000,000 minimum.
            // (2.1jt - 2jt) * 3 * 0.7 = 210,000 raw, well under the 2jt minimum.
            CustomerEntity customer = customer(new BigDecimal("2100000"), new BigDecimal("2000000"), null);

            UserPlafondEntity result = service.calculateAndAssign(customer);

            assertThat(result.getLimitEfektif()).isEqualByComparingTo(MINIMUM_PLAFOND);
        }
    }

    @Nested
    class Recalculate {

        @BeforeEach
        void stubTiers() {
            when(plafondRepository.findByStatusOrderByLimitMaksimalAsc("ACTIVE"))
                    .thenReturn(defaultTierCatalog());
        }

        @Test
        void updatesExistingRow_inPlace_insteadOfInserting() {
            CustomerEntity customer = customer(new BigDecimal("5000000"), new BigDecimal("1000000"), "SWASTA");
            UserPlafondEntity existing = UserPlafondEntity.builder()
                    .idUserPlafond(UUID.randomUUID())
                    .customer(customer)
                    .plafond(tier("Bronze", "5000000"))
                    .limitEfektif(MINIMUM_PLAFOND)
                    .status("ACTIVE")
                    .build();
            when(userPlafondRepository.findByCustomer_Id(customer.getId())).thenReturn(Optional.of(existing));

            UserPlafondEntity result = service.recalculate(customer);

            assertThat(result).isSameAs(existing);
            assertThat(result.getLimitEfektif()).isEqualByComparingTo("12000000");
            assertThat(result.getPlafond().getNamaPlafond()).isEqualTo("Gold");
        }

        @Test
        void delegatesToCalculateAndAssign_whenNoExistingRow() {
            CustomerEntity customer = customer(new BigDecimal("5000000"), new BigDecimal("1000000"), "SWASTA");
            when(userPlafondRepository.findByCustomer_Id(customer.getId())).thenReturn(Optional.empty());

            UserPlafondEntity result = service.recalculate(customer);

            assertThat(result).isNotNull();
            assertThat(result.getLimitEfektif()).isEqualByComparingTo("12000000");
        }
    }

    @Nested
    class TierName {

        @Test
        void returnsTierName_whenAssigned() {
            CustomerEntity customer = customer(new BigDecimal("5000000"), new BigDecimal("1000000"), "SWASTA");
            UserPlafondEntity assigned = UserPlafondEntity.builder()
                    .plafond(tier("Gold", "25000000"))
                    .build();
            when(userPlafondRepository.findByCustomer_Id(customer.getId())).thenReturn(Optional.of(assigned));

            assertThat(service.getTierName(customer)).isEqualTo("Gold");
        }

        @Test
        void returnsNull_whenNotAssignedYet() {
            CustomerEntity customer = customer(new BigDecimal("5000000"), new BigDecimal("1000000"), "SWASTA");
            when(userPlafondRepository.findByCustomer_Id(customer.getId())).thenReturn(Optional.empty());

            assertThat(service.getTierName(customer)).isNull();
        }
    }
}
