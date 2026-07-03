package com.seenears.push.fcm;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Configuration
@EnableConfigurationProperties(FcmPushProperties.class)
public class FirebaseConfig {

    @Bean
    @ConditionalOnProperty(prefix = "app.push.fcm", name = "enabled", havingValue = "true")
    public FirebaseApp firebaseApp(FcmPushProperties properties) throws IOException {
        if (!properties.hasServiceAccountPath()) {
            throw new IllegalStateException("FIREBASE_SERVICE_ACCOUNT_PATH is required when FCM is enabled.");
        }

        try (InputStream serviceAccount = new FileInputStream(properties.serviceAccountPath())) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                return FirebaseApp.initializeApp(options);
            }
            return FirebaseApp.getInstance();
        }
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.push.fcm", name = "enabled", havingValue = "true")
    public FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
        return FirebaseMessaging.getInstance(firebaseApp);
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.push.fcm", name = "enabled", havingValue = "false", matchIfMissing = true)
    public FcmNotificationSender noopFcmNotificationSender() {
        return (notification, tokens) -> FcmSendResult.failure(tokens);
    }
}
