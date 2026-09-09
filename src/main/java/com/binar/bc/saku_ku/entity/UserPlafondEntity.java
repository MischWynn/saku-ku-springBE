package com.binar.bc.saku_ku.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tbl_user_plafond")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPlafondEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_user_plafond")
    private UUID idUserPlafond;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_customer", nullable = false)
    private CustomerEntity customer; // sesuaikan nama class customer-mu

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_plafond", nullable = false)
    private PlafondEntity plafond;

    @Column(name = "limit_efektif", precision = 18, scale = 2)
    private BigDecimal limitEfektif;

    @Column(name = "tanggal_daftar", nullable = false)
    private LocalDateTime tanggalDaftar;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE";

    @PrePersist
    void prePersist() {
        tanggalDaftar = LocalDateTime.now();
    }
}