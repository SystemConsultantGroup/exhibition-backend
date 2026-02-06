package kr.ac.skku.scg.exhibition.media.service;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import kr.ac.skku.scg.exhibition.global.error.NotFoundException;
import kr.ac.skku.scg.exhibition.global.infra.minio.MinioProperties;
import kr.ac.skku.scg.exhibition.media.domain.MediaAssetEntity;
import kr.ac.skku.scg.exhibition.media.repository.MediaAssetRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MediaService {

    private final MediaAssetRepository mediaAssetRepository;
    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    public List<MediaAssetEntity> listByItem(UUID itemId) {
        return mediaAssetRepository.findByItemIdOrderByCreatedAtAsc(itemId);
    }

    public MediaAssetEntity get(UUID mediaId) {
        return mediaAssetRepository.findById(mediaId)
            .orElseThrow(() -> new NotFoundException("Media not found"));
    }

    public MediaObject getObject(UUID mediaId) {
        MediaAssetEntity media = get(mediaId);
        try {
            InputStream objectStream = minioClient.getObject(GetObjectArgs.builder()
                .bucket(minioProperties.bucket())
                .object(media.getObjectKey())
                .build());

            return new MediaObject(media, objectStream.readAllBytes());
        } catch (Exception ex) {
            throw new NotFoundException("Media object not found in MinIO");
        }
    }

    public record MediaObject(MediaAssetEntity media, byte[] bytes) {
    }
}
