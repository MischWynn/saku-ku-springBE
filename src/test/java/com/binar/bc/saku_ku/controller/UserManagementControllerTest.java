package com.binar.bc.saku_ku.controller;

import com.binar.bc.saku_ku.dto.ApiResponse;
import com.binar.bc.saku_ku.dto.ForgotPasswordRequest;
import com.binar.bc.saku_ku.dto.RegisterRequest;
import com.binar.bc.saku_ku.dto.ResetPasswordRequest;
import com.binar.bc.saku_ku.entity.UserEntity;
import com.binar.bc.saku_ku.service.UserManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserManagementControllerTest {

    @Mock
    private UserManagementService userManagementService;

    private UserManagementController controller;

    @BeforeEach
    void setUp() {
        controller = new UserManagementController(userManagementService);
    }

    @Test
    void forgotPassword_delegatesToService() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        when(userManagementService.requestForgotPassword(request)).thenReturn("token-abc");

        assertThat(controller.forgotPassword(request).getData()).isEqualTo("token-abc");
    }

    @Test
    void resetPassword_delegatesToService() {
        ResetPasswordRequest request = new ResetPasswordRequest();

        ApiResponse<String> response = controller.resetPassword(request);

        verify(userManagementService).resetPassword(request);
        assertThat(response.getStatusCode()).isEqualTo(200);
    }

    @Test
    void createUser_delegatesToService() {
        RegisterRequest request = new RegisterRequest();
        UserEntity created = new UserEntity();
        when(userManagementService.createUser(request)).thenReturn(created);

        assertThat(controller.createUser(request).getData()).isSameAs(created);
    }
}
