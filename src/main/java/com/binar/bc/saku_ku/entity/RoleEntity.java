package com.binar.bc.saku_ku.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tbl_role")
@Getter
@Setter

public class RoleEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID) 
    @Column(name = "id_role", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "nama_role", nullable = false, length = 100)
    private String namaRole;

    @Column(name = "description", nullable = true, length = 255)
    private String description;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdDate;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedDate;

    @Column(name = "deleted_date", insertable = false)
    private LocalDateTime deletedDate;
}
