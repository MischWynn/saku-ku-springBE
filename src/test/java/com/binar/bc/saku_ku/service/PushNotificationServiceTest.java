package com.binar.bc.saku_ku.service;

import com.binar.bc.saku_ku.entity.CustomerEntity;
import org.junit.jupiter.api.Test;

import java.util.UUID;

class PushNotificationServiceTest {

    private final PushNotificationService service = new PushNotificationService();

    private CustomerEntity customer(String fcmToken) {
        CustomerEntity customer = new CustomerEntity();
        customer.setId(UUID.randomUUID());
        customer.setFcmToken(fcmToken);
        return customer;
    }

    @Test
    void send_returnsEarly_whenTokenNull() {
        // No FirebaseMessaging.getInstance() call should be attempted - would throw in a plain
        // unit test context since no FirebaseApp is initialized here.
        service.send(customer(null), "Judul", "Pesan", "pengajuan-id");
    }

    @Test
    void send_returnsEarly_whenTokenBlank() {
        service.send(customer("   "), "Judul", "Pesan", "pengajuan-id");
    }

    @Test
    void send_swallowsIllegalStateException_whenFirebaseNotInitialized() {
        // No Spring context here, so FirebaseApp's default instance is never initialized -
        // FirebaseMessaging.getInstance() throws IllegalStateException, which send() must catch.
        service.send(customer("device-token"), "Judul", "Pesan", null);
    }
}
