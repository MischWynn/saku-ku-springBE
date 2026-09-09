package com.binar.bc.saku_ku.service;

import com.binar.bc.saku_ku.dto.RoleMenuRequest;
import com.binar.bc.saku_ku.entity.MenuEntity;
import com.binar.bc.saku_ku.entity.RoleEntity;
import com.binar.bc.saku_ku.entity.RoleMenuEntity;
import com.binar.bc.saku_ku.repository.MenuRepository;
import com.binar.bc.saku_ku.repository.RoleMenuRepository;
import com.binar.bc.saku_ku.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Master Access — role X boleh ngapain aja (view/create/update/delete) di menu Y.
 * Pola "boolean flags" di tbl_role_menu (bukan entity Permission terpisah) — keputusan
 * disetujui user 2 Sept 2026, lihat diagram perbandingan opsi di riwayat chat.
 */
@Service
@RequiredArgsConstructor
public class RoleMenuService {

    private final RoleMenuRepository roleMenuRepository;
    private final RoleRepository roleRepository;
    private final MenuRepository menuRepository;

    public List<RoleMenuEntity> getByRole(UUID roleId) {
        return roleMenuRepository.findByRole_Id(roleId);
    }

    @Transactional
    public RoleMenuEntity upsert(RoleMenuRequest request) {
        RoleMenuEntity roleMenu = roleMenuRepository
                .findByRole_IdAndMenu_Id(request.getRoleId(), request.getMenuId())
                .orElseGet(RoleMenuEntity::new);

        if (roleMenu.getId() == null) {
            RoleEntity role = roleRepository.findById(request.getRoleId())
                    .orElseThrow(() -> new RuntimeException("Role not found: " + request.getRoleId()));
            MenuEntity menu = menuRepository.findById(request.getMenuId())
                    .orElseThrow(() -> new RuntimeException("Menu not found: " + request.getMenuId()));
            roleMenu.setRole(role);
            roleMenu.setMenu(menu);
        }

        roleMenu.setCanView(Boolean.TRUE.equals(request.getCanView()));
        roleMenu.setCanCreate(Boolean.TRUE.equals(request.getCanCreate()));
        roleMenu.setCanUpdate(Boolean.TRUE.equals(request.getCanUpdate()));
        roleMenu.setCanDelete(Boolean.TRUE.equals(request.getCanDelete()));

        return roleMenuRepository.save(roleMenu);
    }

    @Transactional
    public List<RoleMenuEntity> upsertBulk(List<RoleMenuRequest> requests) {
        return requests.stream().map(this::upsert).toList();
    }
}
