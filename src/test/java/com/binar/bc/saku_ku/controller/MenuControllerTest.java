package com.binar.bc.saku_ku.controller;

import com.binar.bc.saku_ku.dto.ApiResponse;
import com.binar.bc.saku_ku.dto.MenuRequest;
import com.binar.bc.saku_ku.entity.MenuEntity;
import com.binar.bc.saku_ku.service.MenuService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MenuControllerTest {

    @Mock
    private MenuService menuService;

    private MenuController controller;

    @BeforeEach
    void setUp() {
        controller = new MenuController(menuService);
    }

    @Test
    void getAllMenus_wrapsServiceResult() {
        MenuEntity menu = new MenuEntity();
        when(menuService.getAllMenus()).thenReturn(List.of(menu));

        assertThat(controller.getAllMenus().getData()).containsExactly(menu);
    }

    @Test
    void createMenu_delegatesToService() {
        MenuRequest request = new MenuRequest();
        MenuEntity created = new MenuEntity();
        when(menuService.createMenu(request)).thenReturn(created);

        assertThat(controller.createMenu(request).getData()).isSameAs(created);
    }

    @Test
    void updateMenu_delegatesToService() {
        UUID id = UUID.randomUUID();
        MenuRequest request = new MenuRequest();
        MenuEntity updated = new MenuEntity();
        when(menuService.updateMenu(id, request)).thenReturn(updated);

        assertThat(controller.updateMenu(id, request).getData()).isSameAs(updated);
    }

    @Test
    void deleteMenu_delegatesToService() {
        UUID id = UUID.randomUUID();

        ApiResponse<String> response = controller.deleteMenu(id);

        verify(menuService).deleteMenu(id);
        assertThat(response.getStatusCode()).isEqualTo(200);
    }
}
