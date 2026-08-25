package com.binar.bc.saku_ku.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tbl_review_log")
@Getter
@Setter
public class ReviewLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_review_log")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_pengajuan", referencedColumnName = "id_pengajuan", nullable = false)
    private PengajuanEntity pengajuan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_user", referencedColumnName = "id_user", nullable = false)
    private UserEntity user;

    @Column(name = "role_name", nullable = false, length = 50)
    private String roleName;

    @Column(name = "action", nullable = false, length = 30)
    private String action;

    @Column(name = "status_from", length = 30)
    private String statusFrom;

    @Column(name = "status_to", length = 30)
    private String statusTo;

    @Column(name = "catatan", columnDefinition = "TEXT")
    private String catatan;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}