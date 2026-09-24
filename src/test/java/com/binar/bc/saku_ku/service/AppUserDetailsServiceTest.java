package com.binar.bc.saku_ku.service;

import com.binar.bc.saku_ku.entity.AppUserEntity;
import com.binar.bc.saku_ku.entity.RoleEntity;
import com.binar.bc.saku_ku.entity.UserEntity;
import com.binar.bc.saku_ku.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    private AppUserDetailsService service;

    @BeforeEach
    void setUp() {
        service = new AppUserDetailsService(userRepository);
    }

    private UserEntity user() {
        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setUsername("dewi.marketing");
        user.setPasswordHash("hashed");
        RoleEntity role = new RoleEntity();
        role.setNamaRole("MARKETING");
        user.setRole(role);
        return user;
    }

    @Test
    void findUser_findsByEmail_whenMatches() {
        UserEntity user = user();
        when(userRepository.findByEmailAndDeletedDateIsNull("dewi@mail.com")).thenReturn(Optional.of(user));

        Optional<AppUserEntity> result = service.findUser("dewi@mail.com");

        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("dewi.marketing");
        assertThat(result.get().getRole()).isEqualTo("MARKETING");
    }

    @Test
    void findUser_fallsBackToUsername_whenEmailNotFound() {
        UserEntity user = user();
        when(userRepository.findByEmailAndDeletedDateIsNull("dewi.marketing")).thenReturn(Optional.empty());
        when(userRepository.findByUsernameAndDeletedDateIsNull("dewi.marketing")).thenReturn(Optional.of(user));

        Optional<AppUserEntity> result = service.findUser("dewi.marketing");

        assertThat(result).isPresent();
        assertThat(result.get().getPassword()).isEqualTo("hashed");
    }

    @Test
    void findUser_returnsEmpty_whenNeitherMatches() {
        when(userRepository.findByEmailAndDeletedDateIsNull("ghost")).thenReturn(Optional.empty());
        when(userRepository.findByUsernameAndDeletedDateIsNull("ghost")).thenReturn(Optional.empty());

        assertThat(service.findUser("ghost")).isEmpty();
    }

    @Test
    void loadUserByUsername_returnsAppUser_whenFound() {
        UserEntity user = user();
        when(userRepository.findByEmailAndDeletedDateIsNull("dewi.marketing")).thenReturn(Optional.empty());
        when(userRepository.findByUsernameAndDeletedDateIsNull("dewi.marketing")).thenReturn(Optional.of(user));

        assertThat(service.loadUserByUsername("dewi.marketing").getUsername()).isEqualTo("dewi.marketing");
    }

    @Test
    void loadUserByUsername_throws_whenNotFound() {
        when(userRepository.findByEmailAndDeletedDateIsNull("ghost")).thenReturn(Optional.empty());
        when(userRepository.findByUsernameAndDeletedDateIsNull("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("ghost"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
