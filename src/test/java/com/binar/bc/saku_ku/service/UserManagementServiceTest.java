package com.binar.bc.saku_ku.service;

import com.binar.bc.saku_ku.dto.ChangePasswordRequest;
import com.binar.bc.saku_ku.dto.ForgotPasswordRequest;
import com.binar.bc.saku_ku.dto.RegisterRequest;
import com.binar.bc.saku_ku.dto.ResetPasswordRequest;
import com.binar.bc.saku_ku.entity.RoleEntity;
import com.binar.bc.saku_ku.entity.UserEntity;
import com.binar.bc.saku_ku.exception.BusinessRuleException;
import com.binar.bc.saku_ku.exception.UnauthorizedException;
import com.binar.bc.saku_ku.repository.RoleRepository;
import com.binar.bc.saku_ku.repository.UserRepository;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserManagementServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private JwtService jwtService;
    @Mock
    private Claims claims;

    private UserManagementService service;

    @BeforeEach
    void setUp() {
        service = new UserManagementService(userRepository, passwordEncoder, roleRepository, jwtService);
    }

    @Test
    void requestForgotPassword_issuesToken_whenEmailFound() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("staff@mail.com");
        when(userRepository.findByEmailAndDeletedDateIsNull("staff@mail.com"))
                .thenReturn(Optional.of(new UserEntity()));
        when(jwtService.issueResetToken(eq("staff@mail.com"), any(Instant.class))).thenReturn("token-abc");

        String result = service.requestForgotPassword(request);

        assertThat(result).isEqualTo("token-abc");
    }

    @Test
    void requestForgotPassword_throws_whenEmailNotFound() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("ghost@mail.com");
        when(userRepository.findByEmailAndDeletedDateIsNull("ghost@mail.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.requestForgotPassword(request)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void resetPassword_updatesPasswordHash_whenTokenValid() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("token-abc");
        request.setNewPassword("NewPassword123!");
        UserEntity user = new UserEntity();
        when(jwtService.parseResetToken("token-abc")).thenReturn(claims);
        when(claims.getSubject()).thenReturn("staff@mail.com");
        when(userRepository.findByEmailAndDeletedDateIsNull("staff@mail.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("NewPassword123!")).thenReturn("hashed");

        service.resetPassword(request);

        assertThat(user.getPasswordHash()).isEqualTo("hashed");
    }

    @Test
    void resetPassword_throws_whenEmailFromTokenNotFound() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("token-abc");
        when(jwtService.parseResetToken("token-abc")).thenReturn(claims);
        when(claims.getSubject()).thenReturn("ghost@mail.com");
        when(userRepository.findByEmailAndDeletedDateIsNull("ghost@mail.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resetPassword(request)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void changePassword_updatesHash_whenOldPasswordMatches() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("old");
        request.setNewPassword("new");
        UserEntity user = new UserEntity();
        user.setPasswordHash("old-hash");
        when(userRepository.findByUsernameAndDeletedDateIsNull("dewi")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old", "old-hash")).thenReturn(true);
        when(passwordEncoder.encode("new")).thenReturn("new-hash");

        service.changePassword("dewi", request);

        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
    }

    @Test
    void changePassword_throws_whenOldPasswordWrong() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("wrong");
        UserEntity user = new UserEntity();
        user.setPasswordHash("old-hash");
        when(userRepository.findByUsernameAndDeletedDateIsNull("dewi")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "old-hash")).thenReturn(false);

        assertThatThrownBy(() -> service.changePassword("dewi", request)).isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void changePassword_throws_whenUserNotFound() {
        when(userRepository.findByUsernameAndDeletedDateIsNull("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.changePassword("ghost", new ChangePasswordRequest()))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void createUser_savesWithEncodedPasswordAndRole() {
        RegisterRequest request = new RegisterRequest();
        request.setNamaLengkap("Dewi");
        request.setUsername("dewi.marketing");
        request.setEmail("dewi@mail.com");
        request.setPassword("plain");
        request.setRoleName("MARKETING");
        RoleEntity role = new RoleEntity();
        role.setNamaRole("MARKETING");
        when(roleRepository.findByNamaRole("MARKETING")).thenReturn(Optional.of(role));
        when(passwordEncoder.encode("plain")).thenReturn("hashed");
        when(userRepository.save(any(UserEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        UserEntity result = service.createUser(request);

        assertThat(result.getUsername()).isEqualTo("dewi.marketing");
        assertThat(result.getPasswordHash()).isEqualTo("hashed");
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
        assertThat(result.getRole()).isSameAs(role);
    }

    @Test
    void createUser_throws_whenRoleNotFound() {
        RegisterRequest request = new RegisterRequest();
        request.setRoleName("GHOST");
        when(roleRepository.findByNamaRole("GHOST")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createUser(request)).isInstanceOf(BusinessRuleException.class);
    }
}
