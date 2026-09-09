package com.binar.bc.saku_ku.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tbl_menu")
@Getter
@Setter
public class MenuEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_menu")
    private UUID id;

    @Column(name = "nama_menu", nullable = false, length = 100)
    private String namaMenu;

    @Column(name = "path", length = 255)
    private String path;

    @Column(name = "icon", length = 100)
    private String icon;

    @Column(name = "parent_id")
    private UUID parentId;

    @Column(name = "urutan")
    private Integer urutan;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
