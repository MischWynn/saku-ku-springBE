package com.binar.bc.saku_ku.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.UUID;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Entity
@Table(name = "tbl_plafond")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlafondEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_plafond")
    private UUID idPlafond;

    @Column(name = "nama_plafond", nullable = false, length = 100)
    private String namaPlafond;

    @Column(name = "deskripsi", columnDefinition = "TEXT")
    private String deskripsi;

    @Column(name = "tipe", length = 50)
    private String tipe;

    @Column(name = "limit_maksimal", nullable = false, precision = 18, scale = 2)
    private BigDecimal limitMaksimal;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
    }
}