package com.binar.bc.saku_ku.controller;

import com.binar.bc.saku_ku.dto.ApiResponse;
import com.binar.bc.saku_ku.dto.ChangePasswordRequest;
import com.binar.bc.saku_ku.dto.UpdateProfileRequest;
import com.binar.bc.saku_ku.dto.UpdateUserRequest;
import com.binar.bc.saku_ku.dto.UserProfileDTO;
import com.binar.bc.saku_ku.entity.AppUserEntity;
import com.binar.bc.saku_ku.entity.RoleEntity;
import com.binar.bc.saku_ku.entity.UserEntity;
import com.binar.bc.saku_ku.exception.UnauthorizedException;
import com.binar.bc.saku_ku.repository.UserRepository;
import com.binar.bc.saku_ku.service.TokenBlacklistService;
import com.binar.bc.saku_ku.service.UserManagementService;
import com.binar.bc.saku_ku.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserManagementService userManagementService;
    @Mock
    private TokenBlacklistService tokenBlacklistService;
    @Mock
    private HttpServletRequest httpServletRequest;

    private UserController controller;

    @BeforeEach
    void setUp() {
        controller = new UserController(userService, userRepository, userManagementService, tokenBlacklistService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void logout_blacklistsAuthorizationHeader() {
        when(httpServletRequest.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer abc");

        ApiResponse<String> response = controller.logout(httpServletRequest);

        verify(tokenBlacklistService).blacklistFromHeader("Bearer abc");
        assertThat(response.getStatusCode()).isEqualTo(200);
    }

    @Test
    void updateOwnProfile_delegatesToService() {
        AppUserEntity currentUser = new AppUserEntity();
        currentUser.setUsername("dewi.marketing");
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setNamaLengkap("Dewi Baru");
        UserEntity updated = new UserEntity();
        when(userService.updateOwnProfile("dewi.marketing", "Dewi Baru", null)).thenReturn(updated);

        assertThat(controller.updateOwnProfile(currentUser, request).getData()).isSameAs(updated);
    }

    @Test
    void changePassword_resolvesUsernameFromSecurityContext() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("dewi.marketing", null));
        ChangePasswordRequest request = new ChangePasswordRequest();

        ApiResponse<String> response = controller.changePassword(request);

        verify(userManagementService).changePassword("dewi.marketing", request);
        assertThat(response.getStatusCode()).isEqualTo(200);
    }

    @Test
    void updateUserBySuperadmin_delegatesToService() {
        UUID id = UUID.randomUUID();
        UpdateUserRequest request = new UpdateUserRequest();
        request.setNamaLengkap("New");
        request.setEmail("new@mail.com");
        request.setStatus("ACTIVE");
        request.setRoleName("BM");
        UserEntity updated = new UserEntity();
        when(userService.updateUserBySuperadmin(id, "New", "new@mail.com", "ACTIVE", "BM")).thenReturn(updated);

        assertThat(controller.updateUserBySuperadmin(id, request).getData()).isSameAs(updated);
    }

    @Test
    void deleteUserById_delegatesToService() {
        UUID id = UUID.randomUUID();

        ApiResponse<String> response = controller.deleteUserById(id);

        verify(userService).deleteUserById(id);
        assertThat(response.getStatusCode()).isEqualTo(200);
    }

    @Test
    void getCurrentUser_resolvesFromSecurityContext() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("dewi.marketing", null));
        UserEntity user = new UserEntity();
        user.setNamaLengkap("Dewi");
        RoleEntity role = new RoleEntity();
        role.setNamaRole("MARKETING");
        user.setRole(role);
        when(userRepository.findByUsername("dewi.marketing")).thenReturn(Optional.of(user));

        ApiResponse<UserProfileDTO> response = controller.getCurrentUser();

        assertThat(response.getData().getNamaLengkap()).isEqualTo("Dewi");
        assertThat(response.getData().getRoleName()).isEqualTo("MARKETING");
    }

    @Test
    void getCurrentUser_throws_whenUserNotFound() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("ghost", null));
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.getCurrentUser()).isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void getUserById_delegatesToService() {
        UUID id = UUID.randomUUID();
        UserEntity user = new UserEntity();
        when(userService.getUserById(id)).thenReturn(user);

        assertThat(controller.getUserById(id).getData()).isSameAs(user);
    }

    @Test
    void getAllUsers_delegatesToService() {
        UserEntity user = new UserEntity();
        when(userService.getAllUsers()).thenReturn(List.of(user));

        assertThat(controller.getAllUsers().getData()).containsExactly(user);
    }
}
