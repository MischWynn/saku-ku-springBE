package com.binar.bc.saku_ku.controller;

import com.binar.bc.saku_ku.dto.ApiResponse;
import com.binar.bc.saku_ku.dto.RoleMenuRequest;
import com.binar.bc.saku_ku.entity.RoleEntity;
import com.binar.bc.saku_ku.entity.RoleMenuEntity;
import com.binar.bc.saku_ku.entity.UserEntity;
import com.binar.bc.saku_ku.exception.UnauthorizedException;
import com.binar.bc.saku_ku.repository.UserRepository;
import com.binar.bc.saku_ku.service.RoleMenuService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleMenuControllerTest {

    @Mock
    private RoleMenuService roleMenuService;
    @Mock
    private UserRepository userRepository;

    private RoleMenuController controller;

    @BeforeEach
    void setUp() {
        controller = new RoleMenuController(roleMenuService, userRepository);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getMyAccess_resolvesRoleFromJwtSubject() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("dewi.marketing", null));
        UUID roleId = UUID.randomUUID();
        RoleEntity role = new RoleEntity();
        role.setId(roleId);
        UserEntity user = new UserEntity();
        user.setRole(role);
        when(userRepository.findByUsername("dewi.marketing")).thenReturn(Optional.of(user));
        RoleMenuEntity entry = new RoleMenuEntity();
        when(roleMenuService.getByRole(roleId)).thenReturn(List.of(entry));

        ApiResponse<List<RoleMenuEntity>> response = controller.getMyAccess();

        assertThat(response.getData()).containsExactly(entry);
    }

    @Test
    void getMyAccess_throws_whenUserNotFound() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("ghost", null));
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.getMyAccess()).isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void getByRole_delegatesToService() {
        UUID roleId = UUID.randomUUID();
        RoleMenuEntity entry = new RoleMenuEntity();
        when(roleMenuService.getByRole(roleId)).thenReturn(List.of(entry));

        assertThat(controller.getByRole(roleId).getData()).containsExactly(entry);
    }

    @Test
    void saveRoleMenus_delegatesToService() {
        RoleMenuRequest request = new RoleMenuRequest();
        RoleMenuEntity saved = new RoleMenuEntity();
        when(roleMenuService.upsertBulk(List.of(request))).thenReturn(List.of(saved));

        assertThat(controller.saveRoleMenus(List.of(request)).getData()).containsExactly(saved);
    }
}
