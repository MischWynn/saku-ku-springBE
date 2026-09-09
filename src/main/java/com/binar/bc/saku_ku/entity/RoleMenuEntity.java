package com.binar.bc.saku_ku.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tbl_role_menu")
@Getter
@Setter
public class RoleMenuEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_role_menu")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_role", referencedColumnName = "id_role", nullable = false)
    private RoleEntity role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_menu", referencedColumnName = "id_menu", nullable = false)
    private MenuEntity menu;

    @Column(name = "can_view", nullable = false)
    private Boolean canView = false;

    @Column(name = "can_create", nullable = false)
    private Boolean canCreate = false;

    @Column(name = "can_update", nullable = false)
    private Boolean canUpdate = false;

    @Column(name = "can_delete", nullable = false)
    private Boolean canDelete = false;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
