package com.binar.bc.saku_ku.controller;

import com.binar.bc.saku_ku.dto.ApiResponse;
import com.binar.bc.saku_ku.entity.AppCustomerEntity;
import com.binar.bc.saku_ku.entity.NotificationEntity;
import com.binar.bc.saku_ku.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifikasi")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ApiResponse<List<NotificationEntity>> getAll(
            @AuthenticationPrincipal AppCustomerEntity currentCustomer
    ) {
        List<NotificationEntity> list = notificationService.getByCustomer(currentCustomer.getId());
        return ApiResponse.success(list, "Notifikasi berhasil diambil");
    }

    @GetMapping("/unread")
    public ApiResponse<List<NotificationEntity>> getUnread(
            @AuthenticationPrincipal AppCustomerEntity currentCustomer
    ) {
        List<NotificationEntity> list = notificationService.getUnreadByCustomer(currentCustomer.getId());
        return ApiResponse.success(list, "Notifikasi belum dibaca berhasil diambil");
    }

    @GetMapping("/unread/count")
    public ApiResponse<Long> countUnread(
            @AuthenticationPrincipal AppCustomerEntity currentCustomer
    ) {
        long count = notificationService.countUnread(currentCustomer.getId());
        return ApiResponse.success(count, "Jumlah notifikasi belum dibaca berhasil diambil");
    }

    @PatchMapping("/{id}/read")
    public ApiResponse<NotificationEntity> markAsRead(
            @PathVariable UUID id,
            @AuthenticationPrincipal AppCustomerEntity currentCustomer
    ) {
        NotificationEntity notif = notificationService.markAsRead(id, currentCustomer.getId());
        return ApiResponse.success(notif, "Notifikasi ditandai sudah dibaca");
    }
}