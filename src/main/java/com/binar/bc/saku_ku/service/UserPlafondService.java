package com.binar.bc.saku_ku.service;

import com.binar.bc.saku_ku.entity.CustomerEntity;
import com.binar.bc.saku_ku.entity.PlafondEntity;
import com.binar.bc.saku_ku.entity.UserPlafondEntity;
import com.binar.bc.saku_ku.repository.PlafondRepository;
import com.binar.bc.saku_ku.repository.UserPlafondRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserPlafondService {

    private static final BigDecimal INCOME_MULTIPLIER = new BigDecimal("3");
    private static final BigDecimal MINIMUM_PLAFOND = new BigDecimal("2000000");

    private final PlafondRepository plafondRepository;
    private final UserPlafondRepository userPlafondRepository;

    /**
     * Dipanggil sekali pas customer register. Kalau belum ada tier (`tbl_plafond`) yang
     * di-seed sama sekali, sengaja di-skip (return null) daripada gagalin registrasi —
     * customer tetap kebuat, plafond-nya nanti nyusul begitu tier-nya ada.
     */
    @Transactional
    public UserPlafondEntity calculateAndAssign(CustomerEntity customer) {
        List<PlafondEntity> tiers = plafondRepository.findByStatusOrderByLimitMaksimalAsc("ACTIVE");
        if (tiers.isEmpty()) {
            return null;
        }

        UserPlafondEntity userPlafond = new UserPlafondEntity();
        userPlafond.setCustomer(customer);
        userPlafond.setStatus("ACTIVE");
        applyComputedPlafond(customer, userPlafond, tiers);

        return userPlafondRepository.save(userPlafond);
    }

    @Transactional
    public UserPlafondEntity recalculate(CustomerEntity customer) {
        List<PlafondEntity> tiers = plafondRepository.findByStatusOrderByLimitMaksimalAsc("ACTIVE");
        if (tiers.isEmpty()) {
            return null;
        }

        UserPlafondEntity existing = userPlafondRepository.findByCustomer_Id(customer.getId()).orElse(null);
        if (existing == null) {
            return calculateAndAssign(customer);
        }

        applyComputedPlafond(customer, existing, tiers);
        return userPlafondRepository.save(existing);
    }

    private void applyComputedPlafond(CustomerEntity customer, UserPlafondEntity userPlafond, List<PlafondEntity> tiers) {
        BigDecimal computed = computeRawAmount(customer);
        PlafondEntity tier = pickTier(tiers, computed);
        BigDecimal limitEfektif = computed.max(MINIMUM_PLAFOND).min(tier.getLimitMaksimal());

        userPlafond.setPlafond(tier);
        userPlafond.setLimitEfektif(limitEfektif);

        customer.setPlafond(limitEfektif);
    }

    public String getTierName(CustomerEntity customer) {
        return userPlafondRepository.findByCustomer_Id(customer.getId())
                .map(up -> up.getPlafond().getNamaPlafond())
                .orElse(null);
    }

    private BigDecimal computeRawAmount(CustomerEntity customer) {
        if (customer.getPendapatanBulanan() == null) {
            return MINIMUM_PLAFOND;
        }
        BigDecimal income = customer.getPendapatanBulanan();
        BigDecimal debt = customer.getUtangBerjalan() != null ? customer.getUtangBerjalan() : BigDecimal.ZERO;
        BigDecimal netIncome = income.subtract(debt);
        if (netIncome.signum() <= 0) {
            return MINIMUM_PLAFOND;
        }

        BigDecimal base = netIncome.multiply(INCOME_MULTIPLIER);
        BigDecimal multiplier = employmentMultiplier(customer.getTipePekerjaan());
        return base.multiply(multiplier);
    }

    //   ASN_TNI_POLRI, BUMN_BUMD, SWASTA  -> kerja tetap/gaji stabil, skor sama (1.0)
    //   WIRASWASTA, NON_PROFIT            -> income cukup stabil tapi gak segaransi kerja tetap (0.8)
    //   FREELANCE                        -> paling gak stabil di antara yang "kerja" (0.6) —
    //                                        LEBIH RENDAH dari wiraswasta, gak ada revenue stream tetap
    //   TIDAK_BEKERJA                    -> floor defensif (0.3) — kasus netIncome<=0 udah ke-floor
    //                                        minimum plafond duluan di computeRawAmount, ini jaga-jaga
    //                                        kalau ada income (mis. pasif/keluarga) meski status nganggur

    private BigDecimal employmentMultiplier(String tipePekerjaan) {
        if (tipePekerjaan == null) return new BigDecimal("0.7");
        return switch (tipePekerjaan) {
            case "ASN_TNI_POLRI", "BUMN_BUMD", "SWASTA" -> new BigDecimal("1.0");
            case "WIRASWASTA", "NON_PROFIT" -> new BigDecimal("0.8");
            case "FREELANCE" -> new BigDecimal("0.6");
            case "TIDAK_BEKERJA" -> new BigDecimal("0.3");
            // Legacy, sebelum kategori diperluas
            case "KARYAWAN", "PNS" -> new BigDecimal("1.0");
            default -> new BigDecimal("0.7");
        };
    }

    private PlafondEntity pickTier(List<PlafondEntity> tiers, BigDecimal computed) {
        for (PlafondEntity tier : tiers) {
            if (computed.compareTo(tier.getLimitMaksimal()) <= 0) {
                return tier;
            }
        }
        return tiers.get(tiers.size() - 1);
    }
}
