package com.binar.bc.saku_ku.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.binar.bc.saku_ku.dto.*;
import org.springframework.http.ResponseEntity;
import java.time.Instant;
import java.util.Optional;
import com.binar.bc.saku_ku.exception.UnauthorizedException;
import com.binar.bc.saku_ku.entity.AppUserEntity;

@Service
@Transactional
@RequiredArgsConstructor

public class AuthService {

    private final AppUserDetailsService appUserDetailService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public ResponseEntity<AuthResponseDTO> login(LoginRequestDTO loginRequestDTO) {
        Optional<AppUserEntity> found = appUserDetailService.findUser(loginRequestDTO.getUsername());

        if (found.isEmpty() || !passwordEncoder.matches(loginRequestDTO.getPassword(), found.get().getPassword())) {
            throw new UnauthorizedException("Invalid username or password");
        }

        AppUserEntity user = found.get();
        String token = jwtService.issue(user.getUsername(), user.getRole(), Instant.now());
        return ResponseEntity.ok(new AuthResponseDTO(token, user.getRole()));
    }


    // public ResponseEntity<AuthResponseDTO> login(LoginRequestDTO loginRequestDTO) {
    //     var found = userRepository.findByUsername(loginRequestDTO.getUsername());
    //     if(found.isEmpty() || !passwordEncoder.matches(loginRequestDTO.getPassword(), found.get().getPassword())) {
    //         throw new UnauthorizedException("Invalid username or password");
    //     }
        
    //     AppUserEntity user = found.get();
    //     String token = jwtService.issue(user, Instant.now());
    //     return ResponseEntity.ok(new AuthResponseDTO(token));
    // }
    
}

