package com.binar.bc.saku_ku.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tbl_notifikasi")
@Getter
@Setter
public class NotificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_notifikasi")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_customer", referencedColumnName = "id_customer", nullable = false)
    private CustomerEntity customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_pengajuan", referencedColumnName = "id_pengajuan")
    private PengajuanEntity pengajuan;

    @Column(name = "judul", nullable = false, length = 150)
    private String judul;

    @Column(name = "pesan", columnDefinition = "TEXT", nullable = false)
    private String pesan;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}