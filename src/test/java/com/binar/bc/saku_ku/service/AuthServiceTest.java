package com.binar.bc.saku_ku.service;

import com.binar.bc.saku_ku.dto.AuthResponseDTO;
import com.binar.bc.saku_ku.dto.LoginRequestDTO;
import com.binar.bc.saku_ku.entity.AppUserEntity;
import com.binar.bc.saku_ku.exception.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AppUserDetailsService appUserDetailService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;

    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(appUserDetailService, passwordEncoder, jwtService);
    }

    private AppUserEntity appUser() {
        AppUserEntity user = new AppUserEntity();
        user.setId(UUID.randomUUID());
        user.setUsername("dewi.marketing");
        user.setPassword("hashed");
        user.setRole("MARKETING");
        return user;
    }

    @Test
    void login_returnsToken_whenCredentialsValid() {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setUsername("dewi.marketing");
        request.setPassword("plain");
        AppUserEntity user = appUser();
        when(appUserDetailService.findUser("dewi.marketing")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("plain", "hashed")).thenReturn(true);
        when(jwtService.issue(org.mockito.ArgumentMatchers.eq("dewi.marketing"),
                org.mockito.ArgumentMatchers.eq("MARKETING"), any(Instant.class))).thenReturn("jwt-token");

        ResponseEntity<AuthResponseDTO> response = service.login(request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getToken()).isEqualTo("jwt-token");
    }

    @Test
    void login_throws_whenUserNotFound() {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setUsername("ghost");
        request.setPassword("plain");
        when(appUserDetailService.findUser("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(request)).isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void login_throws_whenPasswordWrong() {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setUsername("dewi.marketing");
        request.setPassword("wrong");
        AppUserEntity user = appUser();
        when(appUserDetailService.findUser("dewi.marketing")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> service.login(request)).isInstanceOf(UnauthorizedException.class);
    }
}
