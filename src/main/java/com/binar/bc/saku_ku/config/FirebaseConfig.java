package com.binar.bc.saku_ku.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;


@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${app.firebase.credentials-path}")
    private String credentialsPath;

    @PostConstruct
    public void init() {
        Path path = Path.of(credentialsPath);
        if (!Files.exists(path)) {
            log.warn("Firebase service-account file tidak ditemukan di '{}' - push notification FCM di-skip. " +
                    "Notifikasi in-app (tbl_notifikasi) tetap jalan normal.", credentialsPath);
            return;
        }

        try (FileInputStream serviceAccount = new FileInputStream(path.toFile())) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }
            log.info("Firebase Admin SDK berhasil diinisialisasi dari '{}'", credentialsPath);
        } catch (IOException e) {
            log.warn("Gagal inisialisasi Firebase Admin SDK dari '{}' - push notification FCM di-skip: {}",
                    credentialsPath, e.getMessage());
        }
    }
}
