package com.binar.bc.saku_ku.service;

import com.binar.bc.saku_ku.entity.AppUserEntity;
import com.binar.bc.saku_ku.entity.UserEntity;
import com.binar.bc.saku_ku.repository.UserRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @NonNull
    @Override
    @Transactional(readOnly = true)
    public AppUserEntity loadUserByUsername(@NonNull String identifier) {
        return findUser(identifier)
                .orElseThrow(() -> new UsernameNotFoundException("User " + identifier + " tidak ditemukan"));
    }
    
    @Transactional(readOnly = true)
    public Optional<AppUserEntity> findUser(String identifier) {
        Optional<UserEntity> found = userRepository.findByEmailAndDeletedDateIsNull(identifier);
        if (found.isEmpty()) {
            found = userRepository.findByUsernameAndDeletedDateIsNull(identifier);
        }
        return found.map(this::toAppUser);
    }

    private AppUserEntity toAppUser(UserEntity user) {
        AppUserEntity appUser = new AppUserEntity();
        appUser.setId(user.getId());
        appUser.setUsername(user.getUsername());
        appUser.setPassword(user.getPasswordHash());
        appUser.setRole(user.getRole().getNamaRole());
        return appUser;
    }

    
}