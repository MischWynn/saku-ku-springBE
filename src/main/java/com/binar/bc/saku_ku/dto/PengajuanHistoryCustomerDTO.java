package com.binar.bc.saku_ku.dto;

import com.binar.bc.saku_ku.entity.ReviewLogEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Versi ReviewLogEntity yang aman ditampilin ke customer (timeline Status Pinjaman).
 * SENGAJA gak ikutin field `user` (identitas staff internal - username/nama) - customer
 * cukup tau "direview oleh tim apa" (roleName), bukan siapa orangnya. Dipisah dari
 * endpoint staff (/pengajuan/{id}/history) yang tetap balikin ReviewLogEntity mentah
 * apa adanya, gak diubah.
 */
@Getter
@Setter
public class PengajuanHistoryCustomerDTO {
    private String action;
    private String statusFrom;
    private String statusTo;
    private String catatan;
    private String roleName;
    private LocalDateTime createdAt;

    public static PengajuanHistoryCustomerDTO from(ReviewLogEntity log) {
        PengajuanHistoryCustomerDTO dto = new PengajuanHistoryCustomerDTO();
        dto.setAction(log.getAction());
        dto.setStatusFrom(log.getStatusFrom());
        dto.setStatusTo(log.getStatusTo());
        dto.setCatatan(log.getCatatan());
        dto.setRoleName(log.getRoleName());
        dto.setCreatedAt(log.getCreatedAt());
        return dto;
    }
}
