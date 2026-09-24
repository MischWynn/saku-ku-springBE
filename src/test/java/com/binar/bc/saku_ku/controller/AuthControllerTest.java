package com.binar.bc.saku_ku.controller;

import com.binar.bc.saku_ku.dto.AuthResponseDTO;
import com.binar.bc.saku_ku.dto.LoginRequestDTO;
import com.binar.bc.saku_ku.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    private AuthController controller;

    @BeforeEach
    void setUp() {
        controller = new AuthController(authService);
    }

    @Test
    void loginKaryawan_delegatesToService() {
        LoginRequestDTO request = new LoginRequestDTO();
        ResponseEntity<AuthResponseDTO> expected = ResponseEntity.ok(new AuthResponseDTO("jwt-token"));
        when(authService.login(request)).thenReturn(expected);

        assertThat(controller.loginKaryawan(request)).isSameAs(expected);
    }
}
