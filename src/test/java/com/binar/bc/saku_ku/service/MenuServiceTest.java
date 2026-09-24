package com.binar.bc.saku_ku.service;

import com.binar.bc.saku_ku.dto.MenuRequest;
import com.binar.bc.saku_ku.entity.MenuEntity;
import com.binar.bc.saku_ku.repository.MenuRepository;
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
class MenuServiceTest {

    @Mock
    private MenuRepository menuRepository;

    private MenuService service;

    @BeforeEach
    void setUp() {
        service = new MenuService(menuRepository);
    }

    @Test
    void getAllMenus_returnsAll() {
        MenuEntity menu = new MenuEntity();
        when(menuRepository.findAll()).thenReturn(List.of(menu));

        assertThat(service.getAllMenus()).containsExactly(menu);
    }

    @Test
    void createMenu_defaultsUrutanToZero_whenNull() {
        MenuRequest request = new MenuRequest();
        request.setNamaMenu("Overview");
        request.setPath("/admin/overview");
        request.setIcon("LayoutDashboard");
        when(menuRepository.save(any(MenuEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        MenuEntity result = service.createMenu(request);

        assertThat(result.getNamaMenu()).isEqualTo("Overview");
        assertThat(result.getPath()).isEqualTo("/admin/overview");
        assertThat(result.getUrutan()).isZero();
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void createMenu_usesProvidedUrutan_whenPresent() {
        MenuRequest request = new MenuRequest();
        request.setNamaMenu("Master Staff");
        request.setUrutan(5);
        when(menuRepository.save(any(MenuEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        MenuEntity result = service.createMenu(request);

        assertThat(result.getUrutan()).isEqualTo(5);
    }

    @Test
    void updateMenu_onlyUpdatesNonNullFields() {
        UUID id = UUID.randomUUID();
        MenuEntity existing = new MenuEntity();
        existing.setId(id);
        existing.setNamaMenu("Old");
        existing.setPath("/old");
        when(menuRepository.findById(id)).thenReturn(Optional.of(existing));
        when(menuRepository.save(any(MenuEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        MenuRequest request = new MenuRequest();
        request.setNamaMenu("New");
        // path/icon/parentId/urutan/status left null

        MenuEntity result = service.updateMenu(id, request);

        assertThat(result.getNamaMenu()).isEqualTo("New");
        assertThat(result.getPath()).isEqualTo("/old");
    }

    @Test
    void updateMenu_updatesAllFields_whenAllPresent() {
        UUID id = UUID.randomUUID();
        UUID parentId = UUID.randomUUID();
        MenuEntity existing = new MenuEntity();
        existing.setId(id);
        when(menuRepository.findById(id)).thenReturn(Optional.of(existing));
        when(menuRepository.save(any(MenuEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        MenuRequest request = new MenuRequest();
        request.setNamaMenu("New");
        request.setPath("/new");
        request.setIcon("LucideNew");
        request.setParentId(parentId);
        request.setUrutan(3);
        request.setStatus("INACTIVE");

        MenuEntity result = service.updateMenu(id, request);

        assertThat(result.getNamaMenu()).isEqualTo("New");
        assertThat(result.getPath()).isEqualTo("/new");
        assertThat(result.getIcon()).isEqualTo("LucideNew");
        assertThat(result.getParentId()).isEqualTo(parentId);
        assertThat(result.getUrutan()).isEqualTo(3);
        assertThat(result.getStatus()).isEqualTo("INACTIVE");
    }

    @Test
    void updateMenu_throws_whenNotFound() {
        UUID id = UUID.randomUUID();
        when(menuRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateMenu(id, new MenuRequest())).isInstanceOf(RuntimeException.class);
    }

    @Test
    void deleteMenu_deletes_whenExists() {
        UUID id = UUID.randomUUID();
        when(menuRepository.existsById(id)).thenReturn(true);

        service.deleteMenu(id);

        org.mockito.Mockito.verify(menuRepository).deleteById(id);
    }

    @Test
    void deleteMenu_throws_whenNotExists() {
        UUID id = UUID.randomUUID();
        when(menuRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> service.deleteMenu(id)).isInstanceOf(RuntimeException.class);
    }
}
