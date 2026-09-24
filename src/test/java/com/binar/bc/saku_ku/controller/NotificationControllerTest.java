package com.binar.bc.saku_ku.controller;

import com.binar.bc.saku_ku.entity.AppCustomerEntity;
import com.binar.bc.saku_ku.entity.NotificationEntity;
import com.binar.bc.saku_ku.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    private NotificationController controller;

    private AppCustomerEntity currentCustomer;

    @BeforeEach
    void setUp() {
        controller = new NotificationController(notificationService);
        currentCustomer = new AppCustomerEntity();
        currentCustomer.setId(UUID.randomUUID());
    }

    @Test
    void getAll_delegatesToServiceForCurrentCustomer() {
        NotificationEntity notif = new NotificationEntity();
        when(notificationService.getByCustomer(currentCustomer.getId())).thenReturn(List.of(notif));

        assertThat(controller.getAll(currentCustomer).getData()).containsExactly(notif);
    }

    @Test
    void getUnread_delegatesToServiceForCurrentCustomer() {
        NotificationEntity notif = new NotificationEntity();
        when(notificationService.getUnreadByCustomer(currentCustomer.getId())).thenReturn(List.of(notif));

        assertThat(controller.getUnread(currentCustomer).getData()).containsExactly(notif);
    }

    @Test
    void countUnread_delegatesToServiceForCurrentCustomer() {
        when(notificationService.countUnread(currentCustomer.getId())).thenReturn(4L);

        assertThat(controller.countUnread(currentCustomer).getData()).isEqualTo(4L);
    }

    @Test
    void markAsRead_delegatesToServiceForCurrentCustomer() {
        UUID notifId = UUID.randomUUID();
        NotificationEntity notif = new NotificationEntity();
        when(notificationService.markAsRead(notifId, currentCustomer.getId())).thenReturn(notif);

        assertThat(controller.markAsRead(notifId, currentCustomer).getData()).isSameAs(notif);
    }
}
