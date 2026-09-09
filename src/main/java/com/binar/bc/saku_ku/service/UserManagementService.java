package com.binar.bc.saku_ku.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserManagementService {
        
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final JwtService jwtService;

    public String requestForgotPassword(ForgotPasswordRequest request) {
        // tetap cek dulu emailnya beneran terdaftar, biar nggak generate token buat email random
        userRepository.findByEmailAndDeletedDateIsNull(request.getEmail())
                .orElseThrow(() -> new BusinessRuleException("Email not found"));

        return jwtService.issueResetToken(request.getEmail(), Instant.now());
        // tidak ada userRepository.save() sama sekali di sini!
    }

    public void resetPassword(ResetPasswordRequest request) {
        Claims claims = jwtService.parseResetToken(request.getToken());
        // kalau sampai baris ini, berarti token valid, belum expired, dan memang untuk reset password

        String emailFromToken = claims.getSubject();

        UserEntity user = userRepository.findByEmailAndDeletedDateIsNull(emailFromToken)
                .orElseThrow(() -> new BusinessRuleException("Email not found"));

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user); // save di sini cuma buat update password, bukan buat simpan token
    }   

    public void changePassword(String username, ChangePasswordRequest request) {
        UserEntity user = userRepository.findByUsernameAndDeletedDateIsNull(username)
                .orElseThrow(() -> new UnauthorizedException("User tidak ditemukan"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Password lama salah");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    public UserEntity createUser(RegisterRequest request) {
        RoleEntity role = roleRepository.findByNamaRole(request.getRoleName())
                .orElseThrow(() -> new BusinessRuleException("Role not found"));

        UserEntity user = new UserEntity();
        user.setNamaLengkap(request.getNamaLengkap());
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setStatus("ACTIVE");
        user.setRole(role);

        return userRepository.save(user);
    }
} 

    // public String requestForgotPassword(ForgotPasswordRequest forgotPasswordRequest) {
    //     UserEntity user = userRepository.findByEmailAndDeletedDateIsNull(forgotPasswordRequest.getEmail())
    //     .orElseThrow(() -> new BusinessRuleException("Email Not Found"));

    //     String token = UUID.randomUUID().toString();
    //     user.setResetToken(token);
    //     user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(15));
    //     userRepository.save(user);

    //     return token;
    // }

    // public void resetPassword(ResetPasswordRequest resetPasswordRequest) {
    //     UserEntity user = userRepository.findByEmailAndDeletedDateIsNull(resetPasswordRequest.getEmail())
    //             .orElseThrow(() -> new BusinessRuleException("Email Not Found"));

    //             if (user.getResetToken() == null || !user.getResetToken().equals(resetPasswordRequest.getToken())) {
    //                 throw new UnauthorizedException("Invalid Reset Password Token");
    //             }

    //             if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
    //                 throw new UnauthorizedException("Reset Password Token has expired");
    //             }

    //             user.setPassword(passwordEncoder.encode(resetPasswordRequest.getNewPassword()));
    //             user.setResetToken(null);
    //             user.setResetTokenExpiry(null);
                
    //             userRepository.save(user);
    // }
    