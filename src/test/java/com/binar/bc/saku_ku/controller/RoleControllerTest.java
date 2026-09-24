package com.binar.bc.saku_ku.controller;

import com.binar.bc.saku_ku.dto.ApiResponse;
import com.binar.bc.saku_ku.dto.RoleRequest;
import com.binar.bc.saku_ku.entity.RoleEntity;
import com.binar.bc.saku_ku.service.RoleService;
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
class RoleControllerTest {

    @Mock
    private RoleService roleService;

    private RoleController controller;

    @BeforeEach
    void setUp() {
        controller = new RoleController(roleService);
    }

    @Test
    void getAllRoles_wrapsServiceResult() {
        RoleEntity role = new RoleEntity();
        when(roleService.getAllRoles()).thenReturn(List.of(role));

        ApiResponse<List<RoleEntity>> response = controller.getAllRoles();

        assertThat(response.getData()).containsExactly(role);
    }

    @Test
    void createRole_delegatesToService() {
        RoleRequest request = new RoleRequest();
        RoleEntity created = new RoleEntity();
        when(roleService.createRole(request)).thenReturn(created);

        ApiResponse<RoleEntity> response = controller.createRole(request);

        assertThat(response.getData()).isSameAs(created);
    }

    @Test
    void updateRole_delegatesToService() {
        UUID id = UUID.randomUUID();
        RoleRequest request = new RoleRequest();
        request.setNama("BM");
        request.setDescription("Branch Manager");
        RoleEntity updated = new RoleEntity();
        when(roleService.updateRole("BM", "Branch Manager", id)).thenReturn(updated);

        ApiResponse<RoleEntity> response = controller.updateRole(id, request);

        assertThat(response.getData()).isSameAs(updated);
    }

    @Test
    void deleteRole_delegatesToService() {
        UUID id = UUID.randomUUID();

        ApiResponse<String> response = controller.deleteRole(id);

        verify(roleService).deleteRoleById(id);
        assertThat(response.getStatusCode()).isEqualTo(200);
    }
}
