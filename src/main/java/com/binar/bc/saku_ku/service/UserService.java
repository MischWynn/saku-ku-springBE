package com.binar.bc.saku_ku.service;


import org.springframework.stereotype.Service;

import com.binar.bc.saku_ku.entity.RoleEntity;
import com.binar.bc.saku_ku.entity.UserEntity;
import com.binar.bc.saku_ku.exception.BusinessRuleException;
import com.binar.bc.saku_ku.repository.UserRepository;
import com.binar.bc.saku_ku.repository.RoleRepository;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    
    private final UserRepository userRepository;
    private final RoleService roleService;
    private final RoleRepository roleRepository;

    //Get User By: 
    public UserEntity getUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }

    public UserEntity getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));
    }

    // public UserEntity getUserByEmail(String email) {
    //     return userRepository.findByEmail(email)
    //             .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
    // }

    public List<UserEntity> getAllUsers() {
        return userRepository.findAll();
    }    

    public List<UserEntity> getUsersByRole(String role) {
        return userRepository.findAll().stream()
                .filter(user -> user.getRole() != null && role.equals(user.getRole().getNamaRole()))
                .toList();
    }

    // public void deleteUserByUsername(String username) {
    //     UserEntity user = userRepository.findByUsername(username)
    //             .orElseThrow(() -> new RuntimeException("User not found with username: " + username));
    //     userRepository.delete(user);
    // }

    public void deleteUserById(UUID id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        user.setDeletedDate(LocalDateTime.now());
        userRepository.delete(user);
    }

    @Transactional
    public UserEntity updateOwnProfile(
        String currentUsername, 
        String namaLengkap, 
        String email
    ) 
    {
        UserEntity user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("User not found: " + currentUsername));
        if(namaLengkap != null) {
            user.setNamaLengkap(namaLengkap);
        }
        if (email != null) {
            user.setEmail(email);
        }   
        return userRepository.save(user);
    }


    @Transactional
    public UserEntity updateUserBySuperadmin(
            UUID targetUserId,
            String namaLengkap,
            String email,
            String status,
            String roleName
    ) {
        UserEntity user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + targetUserId));

    
        if (namaLengkap != null) {
            user.setNamaLengkap(namaLengkap);
        }
        if (email != null) {
            user.setEmail(email);
        }
        if (status != null) {
            user.setStatus(status);
        }

        if (roleName != null) {
            RoleEntity roleEntity = roleRepository.findByNamaRole(roleName)
                    .orElseThrow(() -> new BusinessRuleException("Role Not Found: " + roleName));
            user.setRole(roleEntity);
        }
        return userRepository.save(user);
    }

    public List<UserEntity> getActiveUsers() { return userRepository.findUserWhereStatusIsActive(); }


}
