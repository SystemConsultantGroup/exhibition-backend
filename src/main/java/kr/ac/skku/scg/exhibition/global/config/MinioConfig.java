package kr.ac.skku.scg.exhibition.global.config;

import io.minio.MinioClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MinioProperties.class)
public class MinioConfig {

    @Bean
    public MinioClient minioClient(MinioProperties properties) {
        MinioClient.Builder builder = MinioClient.builder()
                .credentials(properties.getAccessKey(), properties.getSecretKey());

        String endpoint = properties.getEndpoint();
        if (endpoint != null && (endpoint.startsWith("http://") || endpoint.startsWith("https://"))) {
            builder.endpoint(endpoint);
        } else {
            int port = properties.getPort() == null ? 9000 : properties.getPort();
            boolean secure = properties.getSecure() != null ? properties.getSecure() : port == 443;
            builder.endpoint(endpoint, port, secure);
        }

        return builder.build();
    }
}
