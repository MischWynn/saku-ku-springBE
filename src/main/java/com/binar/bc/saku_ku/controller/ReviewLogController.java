package com.binar.bc.saku_ku.controller;

import com.binar.bc.saku_ku.config.swagger.SwaggerExamples;
import com.binar.bc.saku_ku.dto.ApiResponse;
import com.binar.bc.saku_ku.entity.ReviewLogEntity;
import com.binar.bc.saku_ku.entity.UserEntity;
import com.binar.bc.saku_ku.exception.UnauthorizedException;
import com.binar.bc.saku_ku.repository.UserRepository;
import com.binar.bc.saku_ku.service.ReviewLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/review-log")
@RequiredArgsConstructor
@Tag(name = "Riwayat Review", description = "Riwayat review pengajuan yang dilakukan staff login sendiri. Dipakai halaman Riwayat Review Saya + notifikasi navbar (dot indicator).")
public class ReviewLogController {

    private final ReviewLogService reviewLogService;
    private final UserRepository userRepository;

    // Riwayat review pengajuan yang udah dilakukan staff login (Marketing/BM/Back Office).
    // Dipakai halaman "Riwayat Review Saya".
    @GetMapping("/me")
    @Operation(summary = "Riwayat review milik sendiri", description = "MARKETING/BM/BACK_OFFICE only - superadmin gak pernah nge-log review (cancel-admin gak lewat ReviewLogService), jadi gak dikasih akses.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Belum login", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.UNAUTHORIZED_TOKEN))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Role gak punya akses (mis. SUPERADMIN)", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.FORBIDDEN)))
    })
    public ApiResponse<List<ReviewLogEntity>> getMyHistory() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("User tidak ditemukan"));

        List<ReviewLogEntity> history = reviewLogService.getByUser(user.getId());
        return ApiResponse.success(history, "Riwayat review berhasil diambil");
    }
}
