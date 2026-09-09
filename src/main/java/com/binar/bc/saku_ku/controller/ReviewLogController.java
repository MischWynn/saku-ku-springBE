package com.binar.bc.saku_ku.controller;

import com.binar.bc.saku_ku.dto.ApiResponse;
import com.binar.bc.saku_ku.entity.ReviewLogEntity;
import com.binar.bc.saku_ku.entity.UserEntity;
import com.binar.bc.saku_ku.exception.UnauthorizedException;
import com.binar.bc.saku_ku.repository.UserRepository;
import com.binar.bc.saku_ku.service.ReviewLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/review-log")
@RequiredArgsConstructor
public class ReviewLogController {

    private final ReviewLogService reviewLogService;
    private final UserRepository userRepository;

    // Riwayat review pengajuan yang udah dilakukan staff login (Marketing/BM/Back Office).
    // Dipakai halaman "Riwayat Review Saya".
    @GetMapping("/me")
    public ApiResponse<List<ReviewLogEntity>> getMyHistory() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("User tidak ditemukan"));

        List<ReviewLogEntity> history = reviewLogService.getByUser(user.getId());
        return ApiResponse.success(history, "Riwayat review berhasil diambil");
    }
}
