package com.binar.bc.saku_ku.service;

import org.springframework.stereotype.Service;
import com.binar.bc.saku_ku.entity.RoleEntity;
import com.binar.bc.saku_ku.dto.RoleRequest;
import com.binar.bc.saku_ku.repository.RoleRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service

public class RoleService {

    private final RoleRepository roleRepository;

    public RoleEntity createRole(RoleRequest roleRequest) {
        RoleEntity role = new RoleEntity();
        role.setNamaRole(roleRequest.getNama());
        role.setDescription(roleRequest.getDescription());
        return roleRepository.save(role);
    }

    public List<RoleEntity> getAllRoles() {
      return roleRepository.findAll();
    }

    public RoleEntity updateRole(
        
        String nama, 
        String description,
        UUID id
    ) 
    {
        RoleEntity role = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Role not found with id: " + id));
        role.setNamaRole(nama);
        role.setDescription(description);
        return roleRepository.save(role);
    }

    public void deleteRoleById(UUID id) {
        RoleEntity role = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Role not found with id: " + id));
        roleRepository.delete(role);
    }

    public void deleteRoleByName(String nama) {
        RoleEntity role = roleRepository.findByNamaRole(nama)
                .orElseThrow(() -> new RuntimeException("Role not found with name: " + nama));
        roleRepository.delete(role);
    }
}
