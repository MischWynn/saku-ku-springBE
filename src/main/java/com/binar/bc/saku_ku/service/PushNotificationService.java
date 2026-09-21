package com.binar.bc.saku_ku.service;

import com.binar.bc.saku_ku.entity.CustomerEntity;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

// Ini kirim push notif pakai FCM, dipanggil dari NotificationService.create(). 
@Slf4j
@Service
public class PushNotificationService {

    public void send(CustomerEntity customer, String title, String body, String pengajuanId) {
        String token = customer.getFcmToken();
        if (token == null || token.isBlank()) {
            return;
        }

        Message.Builder messageBuilder = Message.builder()
                .setToken(token)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .putData("channel", "status");

        if (pengajuanId != null) {
            messageBuilder.putData("pengajuanId", pengajuanId);
        }

        try {
            FirebaseMessaging.getInstance().send(messageBuilder.build());
        } catch (IllegalStateException e) {
            // FirebaseApp belum diinisialisasi (file service-account belum ada) - expected selama setup FCM belum kelar, bukan error yang perlu diributin tiap notifikasi.
            log.debug("FCM belum siap, push ke customer {} di-skip: {}", customer.getId(), e.getMessage());
        } catch (FirebaseMessagingException e) {
            // Token invalid/expired (uninstall app, dst) - log doang, jangan sampai gagalin alur approve/reject/disburse yang lagi jalan di caller.
            log.warn("Gagal kirim push FCM ke customer {}: {}", customer.getId(), e.getMessage());
        }
    }
}
