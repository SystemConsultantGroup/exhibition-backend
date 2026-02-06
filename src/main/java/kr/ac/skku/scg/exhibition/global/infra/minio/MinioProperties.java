package kr.ac.skku.scg.exhibition.global.infra.minio;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.minio")
public record MinioProperties(
    String endpoint,
    Integer port,
    String accessKey,
    String secretKey,
    String bucket
) {
}
