package com.binar.bc.saku_ku.dto;

import com.binar.bc.saku_ku.entity.CustomerEntity;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class CustomerResponseDTO {

    private UUID id;
    private String namaLengkap;
    private String nik;
    private String noHp;
    private String email;
    private String alamat;
    private BigDecimal plafond;
    private String status;
    private LocalDate tanggalLahir;
    private String tipePekerjaan;
    private String pekerjaan;
    private Integer lamaBekerjaBulan;
    private BigDecimal pendapatanBulanan;
    private BigDecimal utangBerjalan;
    // Computed, bukan kolom DB — limit dikurangi yang lagi "ketahan" di pipeline review/DISBURSED.
    // Diisi terpisah oleh caller (lihat CustomerAuthService) karena butuh PengajuanService, DTO ini
    // sengaja tetap murni entity mapper.
    private BigDecimal sisaPlafond;
    // Computed juga — nama tier (Bronze/Silver/Gold/Platinum), bukan flat amount (lihat
    // UserPlafondService.getTierName()). Diisi terpisah oleh caller, sama pola kayak sisaPlafond.
    private String tierPlafond;

    public static CustomerResponseDTO from(CustomerEntity customer) {
        if (customer == null) return null;

        CustomerResponseDTO dto = new CustomerResponseDTO();
        dto.setId(customer.getId());
        dto.setNamaLengkap(customer.getNamaLengkap());
        dto.setNik(customer.getNik());
        dto.setNoHp(customer.getNoHp());
        dto.setEmail(customer.getEmail());
        dto.setAlamat(customer.getAlamat());
        dto.setPlafond(customer.getPlafond());
        dto.setStatus(customer.getStatus());
        dto.setTanggalLahir(customer.getTanggalLahir());
        dto.setTipePekerjaan(customer.getTipePekerjaan());
        dto.setPekerjaan(customer.getPekerjaan());
        dto.setLamaBekerjaBulan(customer.getLamaBekerjaBulan());
        dto.setPendapatanBulanan(customer.getPendapatanBulanan());
        dto.setUtangBerjalan(customer.getUtangBerjalan());
        return dto;
    }
}