package com.binar.bc.saku_ku.service;

import com.binar.bc.saku_ku.entity.CustomerEntity;
import com.binar.bc.saku_ku.entity.NotificationEntity;
import com.binar.bc.saku_ku.entity.PengajuanEntity;
import com.binar.bc.saku_ku.exception.BusinessRuleException;
import com.binar.bc.saku_ku.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private PushNotificationService pushNotificationService;

    private NotificationService service;

    @BeforeEach
    void setUp() {
        service = new NotificationService(notificationRepository, pushNotificationService);
    }

    private CustomerEntity customer(UUID id) {
        CustomerEntity customer = new CustomerEntity();
        customer.setId(id);
        return customer;
    }

    @Test
    void create_savesAndSendsPush_withPengajuanId() {
        CustomerEntity customer = customer(UUID.randomUUID());
        PengajuanEntity pengajuan = new PengajuanEntity();
        UUID pengajuanId = UUID.randomUUID();
        pengajuan.setId(pengajuanId);

        service.create(customer, pengajuan, "Status Diperbarui", "Pengajuan kamu disetujui");

        ArgumentCaptor<NotificationEntity> captor = ArgumentCaptor.forClass(NotificationEntity.class);
        verify(notificationRepository).save(captor.capture());
        NotificationEntity saved = captor.getValue();
        assertThat(saved.getJudul()).isEqualTo("Status Diperbarui");
        assertThat(saved.getIsRead()).isFalse();
        verify(pushNotificationService).send(customer, "Status Diperbarui", "Pengajuan kamu disetujui",
                pengajuanId.toString());
    }

    @Test
    void create_sendsPush_withNullPengajuanId_whenPengajuanNull() {
        CustomerEntity customer = customer(UUID.randomUUID());

        service.create(customer, null, "Info", "pesan umum");

        verify(pushNotificationService).send(customer, "Info", "pesan umum", null);
    }

    @Test
    void getByCustomer_delegatesToRepository() {
        UUID customerId = UUID.randomUUID();
        NotificationEntity notif = new NotificationEntity();
        when(notificationRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)).thenReturn(List.of(notif));

        assertThat(service.getByCustomer(customerId)).containsExactly(notif);
    }

    @Test
    void getUnreadByCustomer_delegatesToRepository() {
        UUID customerId = UUID.randomUUID();
        NotificationEntity notif = new NotificationEntity();
        when(notificationRepository.findByCustomerIdAndIsReadFalseOrderByCreatedAtDesc(customerId))
                .thenReturn(List.of(notif));

        assertThat(service.getUnreadByCustomer(customerId)).containsExactly(notif);
    }

    @Test
    void countUnread_delegatesToRepository() {
        UUID customerId = UUID.randomUUID();
        when(notificationRepository.countByCustomerIdAndIsReadFalse(customerId)).thenReturn(3L);

        assertThat(service.countUnread(customerId)).isEqualTo(3L);
    }

    @Test
    void markAsRead_marksTrueAndSaves_whenOwnedByCustomer() {
        UUID customerId = UUID.randomUUID();
        UUID notifId = UUID.randomUUID();
        NotificationEntity notif = new NotificationEntity();
        notif.setId(notifId);
        notif.setCustomer(customer(customerId));
        notif.setIsRead(false);
        when(notificationRepository.findById(notifId)).thenReturn(Optional.of(notif));
        when(notificationRepository.save(any(NotificationEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        NotificationEntity result = service.markAsRead(notifId, customerId);

        assertThat(result.getIsRead()).isTrue();
    }

    @Test
    void markAsRead_throws_whenNotificationNotFound() {
        UUID notifId = UUID.randomUUID();
        when(notificationRepository.findById(notifId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markAsRead(notifId, UUID.randomUUID()))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void markAsRead_throws_whenNotOwnedByCustomer() {
        UUID notifId = UUID.randomUUID();
        NotificationEntity notif = new NotificationEntity();
        notif.setId(notifId);
        notif.setCustomer(customer(UUID.randomUUID()));
        when(notificationRepository.findById(notifId)).thenReturn(Optional.of(notif));

        assertThatThrownBy(() -> service.markAsRead(notifId, UUID.randomUUID()))
                .isInstanceOf(BusinessRuleException.class);
    }
}
