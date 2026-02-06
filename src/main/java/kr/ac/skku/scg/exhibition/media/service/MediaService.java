package kr.ac.skku.scg.exhibition.media.service;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.errors.MinioException;
import io.minio.GetObjectResponse;
import java.io.IOException;
import java.util.UUID;
import kr.ac.skku.scg.exhibition.global.error.NotFoundException;
import kr.ac.skku.scg.exhibition.global.config.MinioProperties;
import kr.ac.skku.scg.exhibition.media.domain.MediaAssetEntity;
import kr.ac.skku.scg.exhibition.media.dto.response.MediaFileResponse;
import kr.ac.skku.scg.exhibition.media.repository.MediaAssetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MediaService {

    private final MediaAssetRepository mediaAssetRepository;
    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    public MediaService(
            MediaAssetRepository mediaAssetRepository,
            MinioClient minioClient,
            MinioProperties minioProperties
    ) {
        this.mediaAssetRepository = mediaAssetRepository;
        this.minioClient = minioClient;
        this.minioProperties = minioProperties;
    }

    public MediaFileResponse getFile(UUID id) {
        MediaAssetEntity media = mediaAssetRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Media not found: " + id));

        try (GetObjectResponse object = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(minioProperties.getBucket())
                        .object(media.getObjectKey())
                        .build())) {

            byte[] bytes = object.readAllBytes();
            String fileName = extractFileName(media.getObjectKey());
            return new MediaFileResponse(fileName, media.getMediaType(), media.getSize(), bytes);
        } catch (MinioException | IOException e) {
            throw new IllegalStateException("Failed to read media file from MinIO: " + media.getObjectKey(), e);
        } catch (Exception e) {
            throw new IllegalStateException("Unexpected MinIO error: " + media.getObjectKey(), e);
        }
    }

    private String extractFileName(String objectKey) {
        int idx = objectKey.lastIndexOf('/');
        return idx >= 0 ? objectKey.substring(idx + 1) : objectKey;
    }
}
