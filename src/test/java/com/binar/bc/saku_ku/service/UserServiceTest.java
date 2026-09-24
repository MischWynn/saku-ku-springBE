package com.binar.bc.saku_ku.service;

import com.binar.bc.saku_ku.entity.RoleEntity;
import com.binar.bc.saku_ku.entity.UserEntity;
import com.binar.bc.saku_ku.exception.BusinessRuleException;
import com.binar.bc.saku_ku.repository.RoleRepository;
import com.binar.bc.saku_ku.repository.UserRepository;
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
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleService roleService;
    @Mock
    private RoleRepository roleRepository;

    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(userRepository, roleService, roleRepository);
    }

    private UserEntity user(UUID id) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setUsername("dewi.marketing");
        return user;
    }

    @Test
    void getUserById_returnsUser_whenFound() {
        UUID id = UUID.randomUUID();
        UserEntity user = user(id);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        assertThat(service.getUserById(id)).isSameAs(user);
    }

    @Test
    void getUserById_throws_whenNotFound() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getUserById(id)).isInstanceOf(RuntimeException.class);
    }

    @Test
    void getUserByUsername_returnsUser_whenFound() {
        UserEntity user = user(UUID.randomUUID());
        when(userRepository.findByUsername("dewi.marketing")).thenReturn(Optional.of(user));

        assertThat(service.getUserByUsername("dewi.marketing")).isSameAs(user);
    }

    @Test
    void getUserByUsername_throws_whenNotFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getUserByUsername("ghost")).isInstanceOf(RuntimeException.class);
    }

    @Test
    void getAllUsers_returnsAll() {
        UserEntity user = user(UUID.randomUUID());
        when(userRepository.findAll()).thenReturn(List.of(user));

        assertThat(service.getAllUsers()).containsExactly(user);
    }

    @Test
    void getUsersByRole_filtersByRoleName() {
        RoleEntity marketing = new RoleEntity();
        marketing.setNamaRole("MARKETING");
        RoleEntity bm = new RoleEntity();
        bm.setNamaRole("BM");

        UserEntity marketingUser = user(UUID.randomUUID());
        marketingUser.setRole(marketing);
        UserEntity bmUser = user(UUID.randomUUID());
        bmUser.setRole(bm);
        UserEntity noRoleUser = user(UUID.randomUUID());

        when(userRepository.findAll()).thenReturn(List.of(marketingUser, bmUser, noRoleUser));

        List<UserEntity> result = service.getUsersByRole("MARKETING");

        assertThat(result).containsExactly(marketingUser);
    }

    @Test
    void deleteUserById_softDeletes_whenFound() {
        UUID id = UUID.randomUUID();
        UserEntity user = user(id);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.save(any(UserEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        service.deleteUserById(id);

        assertThat(user.getDeletedDate()).isNotNull();
    }

    @Test
    void deleteUserById_throws_whenNotFound() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteUserById(id)).isInstanceOf(RuntimeException.class);
    }

    @Test
    void updateOwnProfile_onlyUpdatesNonNullFields() {
        UserEntity user = user(UUID.randomUUID());
        user.setNamaLengkap("Old Name");
        user.setEmail("old@mail.com");
        when(userRepository.findByUsername("dewi.marketing")).thenReturn(Optional.of(user));
        when(userRepository.save(any(UserEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        UserEntity result = service.updateOwnProfile("dewi.marketing", "New Name", null);

        assertThat(result.getNamaLengkap()).isEqualTo("New Name");
        assertThat(result.getEmail()).isEqualTo("old@mail.com");
    }

    @Test
    void updateOwnProfile_throws_whenNotFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateOwnProfile("ghost", "New", null)).isInstanceOf(RuntimeException.class);
    }

    @Test
    void updateUserBySuperadmin_updatesRole_whenRoleNameProvided() {
        UUID id = UUID.randomUUID();
        UserEntity user = user(id);
        RoleEntity newRole = new RoleEntity();
        newRole.setNamaRole("BM");
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(roleRepository.findByNamaRole("BM")).thenReturn(Optional.of(newRole));
        when(userRepository.save(any(UserEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        UserEntity result = service.updateUserBySuperadmin(id, "New Name", "new@mail.com", "ACTIVE", "BM");

        assertThat(result.getNamaLengkap()).isEqualTo("New Name");
        assertThat(result.getEmail()).isEqualTo("new@mail.com");
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
        assertThat(result.getRole()).isSameAs(newRole);
    }

    @Test
    void updateUserBySuperadmin_skipsRoleUpdate_whenRoleNameNull() {
        UUID id = UUID.randomUUID();
        UserEntity user = user(id);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.save(any(UserEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        UserEntity result = service.updateUserBySuperadmin(id, null, null, null, null);

        assertThat(result.getRole()).isNull();
        org.mockito.Mockito.verifyNoInteractions(roleRepository);
    }

    @Test
    void updateUserBySuperadmin_throws_whenUserNotFound() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateUserBySuperadmin(id, null, null, null, null))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void updateUserBySuperadmin_throws_whenRoleNameNotFound() {
        UUID id = UUID.randomUUID();
        UserEntity user = user(id);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(roleRepository.findByNamaRole("GHOST_ROLE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateUserBySuperadmin(id, null, null, null, "GHOST_ROLE"))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void getActiveUsers_delegatesToRepository() {
        UserEntity user = user(UUID.randomUUID());
        when(userRepository.findUserWhereStatusIsActive()).thenReturn(List.of(user));

        assertThat(service.getActiveUsers()).containsExactly(user);
    }
}
