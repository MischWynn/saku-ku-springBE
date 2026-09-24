package com.binar.bc.saku_ku.service;

import com.binar.bc.saku_ku.dto.RoleMenuRequest;
import com.binar.bc.saku_ku.entity.MenuEntity;
import com.binar.bc.saku_ku.entity.RoleEntity;
import com.binar.bc.saku_ku.entity.RoleMenuEntity;
import com.binar.bc.saku_ku.repository.MenuRepository;
import com.binar.bc.saku_ku.repository.RoleMenuRepository;
import com.binar.bc.saku_ku.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleMenuServiceTest {

    @Mock
    private RoleMenuRepository roleMenuRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private MenuRepository menuRepository;

    private RoleMenuService service;

    @BeforeEach
    void setUp() {
        service = new RoleMenuService(roleMenuRepository, roleRepository, menuRepository);
    }

    @Test
    void getByRole_delegatesToRepository() {
        UUID roleId = UUID.randomUUID();
        RoleMenuEntity entry = new RoleMenuEntity();
        when(roleMenuRepository.findByRole_Id(roleId)).thenReturn(List.of(entry));

        assertThat(service.getByRole(roleId)).containsExactly(entry);
    }

    @Test
    void upsert_createsNewRow_whenNoneExists() {
        UUID roleId = UUID.randomUUID();
        UUID menuId = UUID.randomUUID();
        RoleEntity role = new RoleEntity();
        role.setId(roleId);
        MenuEntity menu = new MenuEntity();
        menu.setId(menuId);

        RoleMenuRequest request = new RoleMenuRequest();
        request.setRoleId(roleId);
        request.setMenuId(menuId);
        request.setCanView(true);
        request.setCanCreate(false);

        when(roleMenuRepository.findByRole_IdAndMenu_Id(roleId, menuId)).thenReturn(Optional.empty());
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
        when(menuRepository.findById(menuId)).thenReturn(Optional.of(menu));
        when(roleMenuRepository.save(any(RoleMenuEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        RoleMenuEntity result = service.upsert(request);

        assertThat(result.getRole()).isSameAs(role);
        assertThat(result.getMenu()).isSameAs(menu);
        assertThat(result.getCanView()).isTrue();
        assertThat(result.getCanCreate()).isFalse();
        assertThat(result.getCanUpdate()).isFalse();
        assertThat(result.getCanDelete()).isFalse();
    }

    @Test
    void upsert_updatesExistingRow_withoutRefetchingRoleOrMenu() {
        UUID roleId = UUID.randomUUID();
        UUID menuId = UUID.randomUUID();
        RoleMenuEntity existing = new RoleMenuEntity();
        existing.setId(UUID.randomUUID());

        RoleMenuRequest request = new RoleMenuRequest();
        request.setRoleId(roleId);
        request.setMenuId(menuId);
        request.setCanView(true);
        request.setCanCreate(true);
        request.setCanUpdate(true);
        request.setCanDelete(true);

        when(roleMenuRepository.findByRole_IdAndMenu_Id(roleId, menuId)).thenReturn(Optional.of(existing));
        when(roleMenuRepository.save(any(RoleMenuEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        RoleMenuEntity result = service.upsert(request);

        assertThat(result).isSameAs(existing);
        assertThat(result.getCanDelete()).isTrue();
        org.mockito.Mockito.verifyNoInteractions(roleRepository, menuRepository);
    }

    @Test
    void upsert_throws_whenRoleNotFound() {
        UUID roleId = UUID.randomUUID();
        UUID menuId = UUID.randomUUID();
        RoleMenuRequest request = new RoleMenuRequest();
        request.setRoleId(roleId);
        request.setMenuId(menuId);

        when(roleMenuRepository.findByRole_IdAndMenu_Id(roleId, menuId)).thenReturn(Optional.empty());
        when(roleRepository.findById(roleId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.upsert(request)).isInstanceOf(RuntimeException.class);
    }

    @Test
    void upsert_throws_whenMenuNotFound() {
        UUID roleId = UUID.randomUUID();
        UUID menuId = UUID.randomUUID();
        RoleMenuRequest request = new RoleMenuRequest();
        request.setRoleId(roleId);
        request.setMenuId(menuId);

        when(roleMenuRepository.findByRole_IdAndMenu_Id(roleId, menuId)).thenReturn(Optional.empty());
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(new RoleEntity()));
        when(menuRepository.findById(menuId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.upsert(request)).isInstanceOf(RuntimeException.class);
    }

    @Test
    void upsertBulk_appliesUpsertToEachRequest() {
        UUID roleId = UUID.randomUUID();
        UUID menuId1 = UUID.randomUUID();
        UUID menuId2 = UUID.randomUUID();

        RoleMenuRequest req1 = new RoleMenuRequest();
        req1.setRoleId(roleId);
        req1.setMenuId(menuId1);
        RoleMenuRequest req2 = new RoleMenuRequest();
        req2.setRoleId(roleId);
        req2.setMenuId(menuId2);

        RoleMenuEntity existing = new RoleMenuEntity();
        existing.setId(UUID.randomUUID());
        when(roleMenuRepository.findByRole_IdAndMenu_Id(any(), any())).thenReturn(Optional.of(existing));
        when(roleMenuRepository.save(any(RoleMenuEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        List<RoleMenuEntity> result = service.upsertBulk(List.of(req1, req2));

        assertThat(result).hasSize(2);
    }
}
