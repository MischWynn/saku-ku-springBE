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

/**
 * Hitung + assign plafond awal customer secara otomatis (gak ada assignment manual —
 * keputusan user, lihat CLAUDE.md section Plafond). Formula (didiskusikan 3-4 Sept 2026):
 *
 *   base = (pendapatan_bulanan - utang_berjalan) x 3
 *   plafond_awal = base x multiplier(tipe_pekerjaan), di-clamp ke tier terdekat (tbl_plafond)
 *
 * Kalau data pendapatan/pekerjaan customer belum lengkap (null, mis. Android app belum
 * pernah ngirim), fallback ke minimum absolut Rp2.000.000 (tier Bronze).
 */
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

        BigDecimal computed = computeRawAmount(customer);
        PlafondEntity tier = pickTier(tiers, computed);
        BigDecimal limitEfektif = computed.max(MINIMUM_PLAFOND).min(tier.getLimitMaksimal());

        UserPlafondEntity userPlafond = new UserPlafondEntity();
        userPlafond.setCustomer(customer);
        userPlafond.setPlafond(tier);
        userPlafond.setLimitEfektif(limitEfektif);
        userPlafond.setStatus("ACTIVE");

        UserPlafondEntity saved = userPlafondRepository.save(userPlafond);

        // Sinkron ke kolom lama tbl_customer.plafond juga, biar tempat lain yang masih
        // baca field itu (bukan tbl_user_plafond) gak nampilin 0 terus.
        customer.setPlafond(limitEfektif);

        return saved;
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

    // KARYAWAN & PNS dianggap setara (income stabil/gajian tetap) — keputusan user 4 Sept 2026.
    private BigDecimal employmentMultiplier(String tipePekerjaan) {
        if (tipePekerjaan == null) return new BigDecimal("0.7");
        return switch (tipePekerjaan) {
            case "KARYAWAN", "PNS" -> new BigDecimal("1.0");
            case "WIRASWASTA" -> new BigDecimal("0.8");
            default -> new BigDecimal("0.7");
        };
    }

    // Tiers udah keurut ASC by limitMaksimal. Ambil tier pertama yang muat, atau tier
    // tertinggi kalau computed amount ngelebihin semua tier yang ada.
    private PlafondEntity pickTier(List<PlafondEntity> tiers, BigDecimal computed) {
        for (PlafondEntity tier : tiers) {
            if (computed.compareTo(tier.getLimitMaksimal()) <= 0) {
                return tier;
            }
        }
        return tiers.get(tiers.size() - 1);
    }
}
