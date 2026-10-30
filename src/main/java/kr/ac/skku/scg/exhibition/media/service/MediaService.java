package kr.ac.skku.scg.exhibition.media.service;

import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.errors.MinioException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import kr.ac.skku.scg.exhibition.exhibition.domain.ExhibitionEntity;
import kr.ac.skku.scg.exhibition.global.config.MinioProperties;
import kr.ac.skku.scg.exhibition.global.error.NotFoundException;
import kr.ac.skku.scg.exhibition.item.domain.ItemEntity;
import kr.ac.skku.scg.exhibition.media.domain.MediaAssetEntity;
import kr.ac.skku.scg.exhibition.media.dto.response.MediaFileResponse;
import kr.ac.skku.scg.exhibition.media.repository.MediaAssetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
    private final Executor uploadExecutor;

    /** 테스트 및 단일 파일 호출의 동기 실행 경로를 제공한다. */
    public MediaService(MediaAssetRepository repository, MinioClient client, MinioProperties properties) {
        this(repository, client, properties, Runnable::run);
    }

    @Autowired
    public MediaService(
            MediaAssetRepository mediaAssetRepository,
            MinioClient minioClient,
            MinioProperties minioProperties,
            @Qualifier("mediaUploadExecutor") Executor uploadExecutor
    ) {
        this.mediaAssetRepository = mediaAssetRepository;
        this.minioClient = minioClient;
        this.minioProperties = minioProperties;
        this.uploadExecutor = uploadExecutor;
    }

    /** 전시에 사용할 이미지 또는 영상을 MinIO와 DB에 저장한다. */
    @Transactional
    public MediaAssetEntity uploadForExhibition(
            ExhibitionEntity exhibition,
            MultipartFile file,
            String expectedType
    ) {
        return upload(exhibition, null, file, exhibition.getSlug() + "/", expectedType);
    }

    /** 아이템 미디어를 전시 slug와 발표회차 경로에 저장한다. */
    @Transactional
    public MediaAssetEntity uploadForItem(ItemEntity item, MultipartFile file, String expectedType) {
        if (file != null && !file.isEmpty() && item.getEventPeriod() == null) {
            throw new IllegalArgumentException("Item media requires an event period");
        }
        String prefix = item.getExhibition().getSlug() + "/"
                + (item.getEventPeriod() == null ? "" : item.getEventPeriod().getId() + "/");
        return upload(item.getExhibition(), item, file, prefix, expectedType);
    }

    public record ItemMediaUpload(ItemEntity item, MultipartFile file, String expectedType) { }
    private record PreparedUpload(String key, String contentType, long size, MultipartFile file) { }

    /** 파일의 확장자와 시그니처를 부작용 없이 사전 검증한다. */
    public void validateUpload(MultipartFile file, String expectedType) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Media file must not be empty");
        String contentType = mediaType(extension(fileName(file.getOriginalFilename())));
        if (!contentType.startsWith(expectedType + "/")) {
            throw new IllegalArgumentException("Media must be an " + expectedType + " file");
        }
        validateSignature(file, contentType);
    }

    /** 네트워크 I/O만 병렬 실행하고 엔티티 접근과 영속화는 호출 스레드에 남긴다. */
    @Transactional
    public List<MediaAssetEntity> uploadForItems(List<ItemMediaUpload> uploads) {
        List<PreparedUpload> prepared = uploads.stream().map(upload -> {
            ItemEntity item = upload.item();
            if (item.getEventPeriod() == null) throw new IllegalArgumentException("Item media requires an event period");
            validateUpload(upload.file(), upload.expectedType());
            String name = fileName(upload.file().getOriginalFilename());
            return new PreparedUpload(item.getExhibition().getSlug() + "/" + item.getEventPeriod().getId()
                    + "/" + name, mediaType(extension(name)),
                    upload.file().getSize(), upload.file());
        }).toList();
        // 기존 키를 그대로 사용한다. 같은 파일의 반복 참조는 한 번만 전송해
        // 동일 객체를 병렬로 덮어쓰는 경합과 불필요한 전송을 방지한다.
        Map<String, PreparedUpload> byKey = new LinkedHashMap<>();
        for (PreparedUpload upload : prepared) {
            PreparedUpload existing = byKey.putIfAbsent(upload.key(), upload);
            if (existing != null && existing.file() != upload.file()) {
                throw new IllegalArgumentException("Different files use the same media path: " + upload.key());
            }
        }
        List<PreparedUpload> unique = new ArrayList<>(byKey.values());
        List<CompletableFuture<Void>> transfers = new ArrayList<>();
        for (PreparedUpload upload : unique) {
            try {
                transfers.add(CompletableFuture.runAsync(() -> putObject(upload), uploadExecutor));
            } catch (RuntimeException ex) {
                transfers.add(CompletableFuture.failedFuture(ex));
                break;
            }
        }
        // 실패해도 이미 시작한 전송이 모두 끝난 후 정리해야 늦게 생성되는 고아 객체가 없다.
        try {
            CompletableFuture.allOf(transfers.toArray(CompletableFuture[]::new)).join();
        } catch (CompletionException ex) {
            for (int i = 0; i < transfers.size(); i++) {
                if (!transfers.get(i).isCompletedExceptionally()) removeObject(unique.get(i).key());
            }
            throw new IllegalStateException("Failed to upload bulk media", ex.getCause());
        }
        // DB 저장 중 오류가 발생해도 모든 성공 객체가 롤백 정리 대상이어야 한다.
        unique.forEach(upload -> removeOnRollback(upload.key()));
        List<MediaAssetEntity> assets = new ArrayList<>();
        for (int i = 0; i < prepared.size(); i++) {
            PreparedUpload upload = prepared.get(i);
            ItemEntity item = uploads.get(i).item();
            assets.add(new MediaAssetEntity(null, item, item.getExhibition(), upload.key(), upload.contentType(), upload.size()));
        }
        return mediaAssetRepository.saveAll(assets);
    }

    /** 엔티티와 DB에 접근하지 않고 파일을 스트리밍 전송한다. */
    private void putObject(PreparedUpload upload) {
        try (InputStream input = upload.file().getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder().bucket(minioProperties.getBucket())
                    .object(upload.key()).stream(input, upload.size(), -1).contentType(upload.contentType()).build());
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to upload media file: " + upload.file().getOriginalFilename(), ex);
        }
    }

    /** 실패한 업로드나 DB 롤백의 객체를 최선 노력으로 정리한다. */
    private void removeObject(String key) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(minioProperties.getBucket()).object(key).build());
        } catch (Exception ignored) {
            // 원래 업로드/DB 오류를 보존한다.
        }
    }

    /** 검증한 미디어 파일을 지정 경로에 저장한다. */
    private MediaAssetEntity upload(ExhibitionEntity exhibition, ItemEntity item, MultipartFile file,
            String prefix, String expectedType) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        validateUpload(file, expectedType);
        String fileName = fileName(file.getOriginalFilename());
        String contentType = mediaType(extension(fileName));
        // 저장 형식은 기존과 동일하다: 전시 slug[/회차 ID]/원본 파일명.
        String key = prefix + fileName;
        putObject(new PreparedUpload(key, contentType, file.getSize(), file));
        removeOnRollback(key);
        return mediaAssetRepository.save(
                new MediaAssetEntity(null, item, exhibition, key, contentType, file.getSize()));
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

    /** 업로드 경로를 제거한 파일명만 반환한다. */
    private String fileName(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("Media file name is required");
        }
        String normalized = raw.replace('\\', '/');
        String fileName = normalized.substring(normalized.lastIndexOf('/') + 1);
        if (fileName.isBlank()) {
            throw new IllegalArgumentException("Media file name is required");
        }
        return fileName;
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
                    removeObject(key);
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
