package com.binar.bc.saku_ku.service;

import com.binar.bc.saku_ku.dto.RoleRequest;
import com.binar.bc.saku_ku.entity.RoleEntity;
import com.binar.bc.saku_ku.repository.RoleRepository;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    private RoleRepository roleRepository;

    private RoleService service;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        service = new RoleService(roleRepository);
    }

    @Test
    void createRole_savesEntityWithRequestFields() {
        RoleRequest request = new RoleRequest();
        request.setNama("MARKETING");
        request.setDescription("Staff marketing");
        when(roleRepository.save(any(RoleEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        RoleEntity result = service.createRole(request);

        assertThat(result.getNamaRole()).isEqualTo("MARKETING");
        assertThat(result.getDescription()).isEqualTo("Staff marketing");
    }

    @Test
    void getAllRoles_returnsAllFromRepository() {
        RoleEntity role = new RoleEntity();
        when(roleRepository.findAll()).thenReturn(List.of(role));

        List<RoleEntity> result = service.getAllRoles();

        assertThat(result).containsExactly(role);
    }

    @Test
    void updateRole_updatesFieldsAndSaves_whenFound() {
        UUID id = UUID.randomUUID();
        RoleEntity existing = new RoleEntity();
        existing.setId(id);
        when(roleRepository.findById(id)).thenReturn(Optional.of(existing));
        when(roleRepository.save(any(RoleEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        RoleEntity result = service.updateRole("BM", "Branch manager", id);

        assertThat(result.getNamaRole()).isEqualTo("BM");
        assertThat(result.getDescription()).isEqualTo("Branch manager");
    }

    @Test
    void updateRole_throws_whenNotFound() {
        UUID id = UUID.randomUUID();
        when(roleRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateRole("BM", "desc", id))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining(id.toString());
    }

    @Test
    void deleteRoleById_deletes_whenFound() {
        UUID id = UUID.randomUUID();
        RoleEntity existing = new RoleEntity();
        existing.setId(id);
        when(roleRepository.findById(id)).thenReturn(Optional.of(existing));

        service.deleteRoleById(id);

        verify(roleRepository).delete(existing);
    }

    @Test
    void deleteRoleById_throws_whenNotFound() {
        UUID id = UUID.randomUUID();
        when(roleRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteRoleById(id)).isInstanceOf(RuntimeException.class);
    }

    @Test
    void deleteRoleByName_deletes_whenFound() {
        RoleEntity existing = new RoleEntity();
        existing.setNamaRole("MARKETING");
        when(roleRepository.findByNamaRole("MARKETING")).thenReturn(Optional.of(existing));

        service.deleteRoleByName("MARKETING");

        verify(roleRepository).delete(existing);
    }

    @Test
    void deleteRoleByName_throws_whenNotFound() {
        when(roleRepository.findByNamaRole("GHOST")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteRoleByName("GHOST")).isInstanceOf(RuntimeException.class);
    }
}
