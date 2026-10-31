package kr.co.jobhub.common.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/** OCI에 읽기 전용으로 연결한 서비스 계정으로 Firebase Admin SDK를 초기화한다. */
@Configuration
@ConditionalOnProperty(name = "jobhub.push.enabled", havingValue = "true")
public class FirebaseMessagingConfig {
    @Bean(destroyMethod = "delete")
    FirebaseApp firebaseApp(@Value("${jobhub.push.credentials-file}") String credentialsFile,
                            @Value("${jobhub.push.project-id}") String projectId) throws IOException {
        Path path = Path.of(credentialsFile).toAbsolutePath().normalize();
        if (!Files.isRegularFile(path)) {
            throw new IllegalStateException("Firebase service-account file was not found: " + path);
        }
        try (InputStream input = Files.newInputStream(path)) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(input))
                    .setProjectId(projectId)
                    .build();
            return FirebaseApp.initializeApp(options, "public-job-hub");
        }
    }

    @Bean
    FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
        return FirebaseMessaging.getInstance(firebaseApp);
    }
}
