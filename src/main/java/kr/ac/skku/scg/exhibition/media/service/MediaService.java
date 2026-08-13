package kr.ac.skku.scg.exhibition.media.service;

import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.errors.MinioException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.UUID;
import kr.ac.skku.scg.exhibition.exhibition.domain.ExhibitionEntity;
import kr.ac.skku.scg.exhibition.global.config.MinioProperties;
import kr.ac.skku.scg.exhibition.global.error.NotFoundException;
import kr.ac.skku.scg.exhibition.media.domain.MediaAssetEntity;
import kr.ac.skku.scg.exhibition.media.dto.response.MediaFileResponse;
import kr.ac.skku.scg.exhibition.media.repository.MediaAssetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

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

    /** 전시에 사용할 이미지 또는 영상을 MinIO와 DB에 저장한다. */
    @Transactional
    public MediaAssetEntity uploadForExhibition(
            ExhibitionEntity exhibition,
            MultipartFile file,
            String kind,
            String expectedType
    ) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        String extension = extension(file.getOriginalFilename());
        String contentType = mediaType(extension);
        if (!contentType.startsWith(expectedType + "/")) {
            throw new IllegalArgumentException(kind + " must be an " + expectedType + " file");
        }
        validateSignature(file, contentType);

        String key = "exhibitions/" + exhibition.getId() + "/" + kind + "/" + UUID.randomUUID() + extension;
        try (InputStream input = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioProperties.getBucket())
                    .object(key)
                    .stream(input, file.getSize(), -1)
                    .contentType(contentType)
                    .build());
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to upload media file: " + file.getOriginalFilename(), ex);
        }
        removeOnRollback(key);
        return mediaAssetRepository.save(
                new MediaAssetEntity(null, null, exhibition, key, contentType, file.getSize()));
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
            String contentType = extractContentType(object, media.getMediaType());
            long contentLength = bytes.length;
            return new MediaFileResponse(fileName, contentType, contentLength, bytes);
        } catch (MinioException | IOException e) {
            throw new IllegalStateException("Failed to read media file from MinIO: " + media.getObjectKey(), e);
        } catch (Exception e) {
            throw new IllegalStateException("Unexpected MinIO error: " + media.getObjectKey(), e);
        }
    }

    /** 파일 확장자를 안전한 소문자 형식으로 반환한다. */
    private String extension(String fileName) {
        int index = fileName == null ? -1 : fileName.lastIndexOf('.');
        if (index < 0) {
            throw new IllegalArgumentException("Media file extension is required");
        }
        return fileName.substring(index).toLowerCase(Locale.ROOT);
    }

    /** 허용한 미디어 확장자의 Content-Type을 반환한다. */
    private String mediaType(String extension) {
        return switch (extension) {
            case ".png" -> "image/png";
            case ".jpg", ".jpeg" -> "image/jpeg";
            case ".gif" -> "image/gif";
            case ".webp" -> "image/webp";
            case ".mp4" -> "video/mp4";
            case ".webm" -> "video/webm";
            case ".mov" -> "video/quicktime";
            default -> throw new IllegalArgumentException("Unsupported media extension: " + extension);
        };
    }

    /** 파일 시그니처가 확장자와 일치하는지 확인한다. */
    private void validateSignature(MultipartFile file, String contentType) {
        byte[] bytes;
        try (InputStream input = file.getInputStream()) {
            bytes = input.readNBytes(12);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Failed to inspect media file", ex);
        }
        boolean valid = switch (contentType) {
            case "image/png" -> startsWith(bytes, 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a);
            case "image/jpeg" -> startsWith(bytes, 0xff, 0xd8, 0xff);
            case "image/gif" -> startsWith(bytes, 'G', 'I', 'F', '8', '7', 'a')
                    || startsWith(bytes, 'G', 'I', 'F', '8', '9', 'a');
            case "image/webp" -> startsWith(bytes, 'R', 'I', 'F', 'F') && matchesAt(bytes, 8, 'W', 'E', 'B', 'P');
            case "video/mp4", "video/quicktime" -> matchesAt(bytes, 4, 'f', 't', 'y', 'p');
            case "video/webm" -> startsWith(bytes, 0x1a, 0x45, 0xdf, 0xa3);
            default -> false;
        };
        if (!valid) {
            throw new IllegalArgumentException("Media file signature does not match its extension");
        }
    }

    /** 바이트 배열이 지정한 시그니처로 시작하는지 확인한다. */
    private boolean startsWith(byte[] bytes, int... signature) {
        return matchesAt(bytes, 0, signature);
    }

    /** 바이트 배열의 지정 위치가 시그니처와 일치하는지 확인한다. */
    private boolean matchesAt(byte[] bytes, int offset, int... signature) {
        if (bytes.length < offset + signature.length) {
            return false;
        }
        for (int index = 0; index < signature.length; index++) {
            if ((bytes[offset + index] & 0xff) != signature[index]) {
                return false;
            }
        }
        return true;
    }

    /** DB 트랜잭션 롤백 시 업로드한 MinIO 객체를 제거한다. */
    private void removeOnRollback(String key) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    try {
                        minioClient.removeObject(RemoveObjectArgs.builder()
                                .bucket(minioProperties.getBucket()).object(key).build());
                    } catch (Exception ignored) {
                        // DB 롤백 원인을 보존한다.
                    }
                }
            }
        });
    }

    private String extractContentType(GetObjectResponse object, String fallbackContentType) {
        String minioContentType = object.headers().get("Content-Type");
        if (minioContentType != null && !minioContentType.isBlank()) {
            return minioContentType;
        }
        if (fallbackContentType != null && !fallbackContentType.isBlank()) {
            return fallbackContentType;
        }
        return "application/octet-stream";
    }

    private String extractFileName(String objectKey) {
        int idx = objectKey.lastIndexOf('/');
        return idx >= 0 ? objectKey.substring(idx + 1) : objectKey;
    }
}
