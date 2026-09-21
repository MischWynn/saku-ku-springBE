package com.binar.bc.saku_ku.service;

import com.binar.bc.saku_ku.entity.CustomerEntity;
import com.binar.bc.saku_ku.entity.NotificationEntity;
import com.binar.bc.saku_ku.entity.PengajuanEntity;
import com.binar.bc.saku_ku.exception.BusinessRuleException;
import com.binar.bc.saku_ku.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final PushNotificationService pushNotificationService;

    // ini buat bikin notifikasi baru, dipanggil dari service lain (misal: pengajuanService) pas ada event yang butuh notifikasi ke customer. Notifikasi disimpan di DB + dikirim ke device via FCM.
    @Transactional
    public void create(CustomerEntity customer, PengajuanEntity pengajuan, String judul, String pesan) {
        NotificationEntity notif = new NotificationEntity();
        notif.setCustomer(customer);
        notif.setPengajuan(pengajuan);
        notif.setJudul(judul);
        notif.setPesan(pesan);
        notif.setIsRead(false);

        notificationRepository.save(notif);

        String pengajuanId = pengajuan != null ? pengajuan.getId().toString() : null;
        pushNotificationService.send(customer, judul, pesan, pengajuanId);
    }

    @Transactional(readOnly = true)
    public List<NotificationEntity> getByCustomer(UUID customerId) {
        return notificationRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    @Transactional(readOnly = true)
    public List<NotificationEntity> getUnreadByCustomer(UUID customerId) {
        return notificationRepository.findByCustomerIdAndIsReadFalseOrderByCreatedAtDesc(customerId);
    }

    @Transactional(readOnly = true)
    public long countUnread(UUID customerId) {
        return notificationRepository.countByCustomerIdAndIsReadFalse(customerId);
    }

    @Transactional
    public NotificationEntity markAsRead(UUID notifId, UUID currentCustomerId) {
        NotificationEntity notif = notificationRepository.findById(notifId)
                .orElseThrow(() -> new BusinessRuleException("Notifikasi tidak ditemukan"));

        if (!notif.getCustomer().getId().equals(currentCustomerId)) {
            throw new BusinessRuleException("Anda tidak berhak mengakses notifikasi ini");
        }

        notif.setIsRead(true);
        return notificationRepository.save(notif);
    }
}