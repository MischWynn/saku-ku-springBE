package com.binar.bc.saku_ku.controller;

import com.binar.bc.saku_ku.config.swagger.SwaggerExamples;
import com.binar.bc.saku_ku.dto.ApiResponse;
import com.binar.bc.saku_ku.entity.AppCustomerEntity;
import com.binar.bc.saku_ku.entity.NotificationEntity;
import com.binar.bc.saku_ku.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifikasi")
@RequiredArgsConstructor
@Tag(name = "Notifikasi", description = "Feed notifikasi customer (Android) - trigger otomatis tiap transisi status pengajuan. Customer-only.")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "Semua notifikasi milik sendiri")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Belum login", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.UNAUTHORIZED_TOKEN)))
    })
    public ApiResponse<List<NotificationEntity>> getAll(
            @AuthenticationPrincipal AppCustomerEntity currentCustomer
    ) {
        List<NotificationEntity> list = notificationService.getByCustomer(currentCustomer.getId());
        return ApiResponse.success(list, "Notifikasi berhasil diambil");
    }

    @GetMapping("/unread")
    @Operation(summary = "Notifikasi belum dibaca")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Belum login", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.UNAUTHORIZED_TOKEN)))
    })
    public ApiResponse<List<NotificationEntity>> getUnread(
            @AuthenticationPrincipal AppCustomerEntity currentCustomer
    ) {
        List<NotificationEntity> list = notificationService.getUnreadByCustomer(currentCustomer.getId());
        return ApiResponse.success(list, "Notifikasi belum dibaca berhasil diambil");
    }

    @GetMapping("/unread/count")
    @Operation(summary = "Jumlah notifikasi belum dibaca", description = "Dipakai badge dot merah di navbar/top bar.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Belum login", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.UNAUTHORIZED_TOKEN)))
    })
    public ApiResponse<Long> countUnread(
            @AuthenticationPrincipal AppCustomerEntity currentCustomer
    ) {
        long count = notificationService.countUnread(currentCustomer.getId());
        return ApiResponse.success(count, "Jumlah notifikasi belum dibaca berhasil diambil");
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Tandai 1 notifikasi sudah dibaca")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Notifikasi ditandai sudah dibaca"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Belum login", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.UNAUTHORIZED_TOKEN))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "ID tidak ditemukan / bukan milik customer ini", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.NOT_FOUND)))
    })
    public ApiResponse<NotificationEntity> markAsRead(
            @PathVariable UUID id,
            @AuthenticationPrincipal AppCustomerEntity currentCustomer
    ) {
        NotificationEntity notif = notificationService.markAsRead(id, currentCustomer.getId());
        return ApiResponse.success(notif, "Notifikasi ditandai sudah dibaca");
    }
}
