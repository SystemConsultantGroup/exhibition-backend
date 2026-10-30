package kr.ac.skku.scg.exhibition.media.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import java.time.Instant;
import java.io.IOException;
import java.util.UUID;
import kr.ac.skku.scg.exhibition.category.domain.CategoryEntity;
import kr.ac.skku.scg.exhibition.eventperiod.domain.EventPeriodEntity;
import kr.ac.skku.scg.exhibition.exhibition.domain.ExhibitionEntity;
import kr.ac.skku.scg.exhibition.global.config.MinioProperties;
import kr.ac.skku.scg.exhibition.item.domain.ItemEntity;
import kr.ac.skku.scg.exhibition.media.domain.MediaAssetEntity;
import kr.ac.skku.scg.exhibition.media.repository.MediaAssetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class MediaServiceTest {

    @Mock private MediaAssetRepository repository;
    @Mock private MinioClient minio;
    private MediaService service;

    @BeforeEach
    void setUp() {
        MinioProperties properties = new MinioProperties();
        properties.setBucket("test");
        service = new MediaService(repository, minio, properties);
    }

    @Test
    void uploadsExhibitionImage() throws Exception {
        ExhibitionEntity exhibition = new ExhibitionEntity(UUID.randomUUID(), "slug", "전시");
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        MockMultipartFile file = new MockMultipartFile("logoFile", "logo.png", "image/png",
                new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a});

        MediaAssetEntity media = service.uploadForExhibition(exhibition, file, "image");

        assertThat(media.getMediaType()).isEqualTo("image/png");
        assertThat(media.getExhibition()).isSameAs(exhibition);
        assertThat(media.getObjectKey()).isEqualTo("slug/logo.png");
        verify(minio).putObject(any(PutObjectArgs.class));
    }

    @Test
    void uploadsItemMediaUnderEventPeriod() throws Exception {
        ExhibitionEntity exhibition = new ExhibitionEntity(UUID.randomUUID(), "slug", "전시");
        EventPeriodEntity period = new EventPeriodEntity(UUID.randomUUID(), exhibition, "2026",
                Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-12-31T00:00:00Z"));
        ItemEntity item = new ItemEntity(UUID.randomUUID(), exhibition,
                new CategoryEntity(UUID.randomUUID(), exhibition, "AI"), period, "작품", null);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        MockMultipartFile file = new MockMultipartFile("thumbnailFile", "thumb.jpg", "image/jpeg",
                new byte[] {(byte) 0xff, (byte) 0xd8, (byte) 0xff});

        MediaAssetEntity media = service.uploadForItem(item, file, "image");

        assertThat(media.getObjectKey()).isEqualTo("slug/" + period.getId() + "/thumb.jpg");
    }

    /** 두 전송이 실제로 겹치며 DB 저장은 호출 스레드에서만 수행한다. */
    @Test
    void batchUploadsConcurrentlyWithoutMovingJpaToWorkers() throws Exception {
        var executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            var properties = new MinioProperties();
            properties.setBucket("test");
            var batch = new MediaService(repository, minio, properties, executor);
            var started = new java.util.concurrent.CountDownLatch(2);
            when(minio.putObject(any())).thenAnswer(call -> {
                started.countDown();
                assertThat(started.await(3, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
                return null;
            });
            Thread caller = Thread.currentThread();
            when(repository.saveAll(any())).thenAnswer(call -> {
                assertThat(Thread.currentThread()).isSameAs(caller);
                return call.getArgument(0);
            });
            var item = itemWithPeriod();
            var first = image();
            var second = new MockMultipartFile("mediaFiles", "second.jpg", "image/jpeg", first.getBytes());
            var assets = batch.uploadForItems(java.util.List.of(
                    new MediaService.ItemMediaUpload(item, first, "image"),
                    new MediaService.ItemMediaUpload(item, second, "image")));
            assertThat(assets).hasSize(2);
            assertThat(assets.get(0).getObjectKey()).isEqualTo("slug/" + item.getEventPeriod().getId() + "/same.jpg");
            assertThat(assets.get(1).getObjectKey()).isEqualTo("slug/" + item.getEventPeriod().getId() + "/second.jpg");
        } finally { executor.shutdownNow(); }
    }

    /** 하나가 실패해도 나머지 전송의 완료를 기다리고 성공 객체만 모두 정리한다. */
    @Test
    void batchFailureWaitsAndCleansSuccessfulTransfers() throws Exception {
        var executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            var properties = new MinioProperties();
            properties.setBucket("test");
            var batch = new MediaService(repository, minio, properties, executor);
            var calls = new java.util.concurrent.atomic.AtomicInteger();
            var succeeded = new java.util.concurrent.atomic.AtomicBoolean();
            when(minio.putObject(any())).thenAnswer(call -> {
                if (calls.incrementAndGet() == 1) throw new IOException("storage failure");
                Thread.sleep(50);
                succeeded.set(true);
                return null;
            });
            assertThatThrownBy(() -> batch.uploadForItems(java.util.List.of(
                    new MediaService.ItemMediaUpload(itemWithPeriod(), image(), "image"),
                    new MediaService.ItemMediaUpload(itemWithPeriod(), image(), "image"))))
                    .isInstanceOf(IllegalStateException.class);
            assertThat(succeeded).isTrue();
            verify(minio).removeObject(any(io.minio.RemoveObjectArgs.class));
            org.mockito.Mockito.verifyNoInteractions(repository);
        } finally { executor.shutdownNow(); }
    }

    /** DB 롤백은 병렬 전송이 끝난 객체를 모두 제거한다. */
    @Test
    void batchRegistersRollbackCleanupBeforeDatabaseSave() throws Exception {
        org.springframework.transaction.support.TransactionSynchronizationManager.initSynchronization();
        try {
            when(repository.saveAll(any())).thenThrow(new IllegalStateException("DB failure"));
            assertThatThrownBy(() -> service.uploadForItems(java.util.List.of(
                    new MediaService.ItemMediaUpload(itemWithPeriod(), image(), "image"))))
                    .isInstanceOf(IllegalStateException.class);
            for (var callback : org.springframework.transaction.support.TransactionSynchronizationManager.getSynchronizations()) {
                callback.afterCompletion(org.springframework.transaction.support.TransactionSynchronization.STATUS_ROLLED_BACK);
            }
            verify(minio).removeObject(any(io.minio.RemoveObjectArgs.class));
        } finally { org.springframework.transaction.support.TransactionSynchronizationManager.clearSynchronization(); }
    }

    /** 같은 파일이 여러 작품/슬롯에서 참조되어도 기존 경로와 각 미디어 레코드를 유지한다. */
    @Test
    void repeatedFileUsesTheOriginalPathAndUploadsOnlyOnce() throws Exception {
        var item = itemWithPeriod();
        var file = image();
        when(repository.saveAll(any())).thenAnswer(call -> call.getArgument(0));
        var assets = service.uploadForItems(java.util.List.of(
                new MediaService.ItemMediaUpload(item, file, "image"),
                new MediaService.ItemMediaUpload(item, file, "image")));
        assertThat(assets).hasSize(2);
        assertThat(assets).extracting(MediaAssetEntity::getObjectKey)
                .containsExactly("slug/" + item.getEventPeriod().getId() + "/same.jpg",
                        "slug/" + item.getEventPeriod().getId() + "/same.jpg");
        verify(minio).putObject(any(PutObjectArgs.class));
    }

    /** 확장자 대소문자/파일명/Content-Type과 실제 전송 바이트를 모두 그대로 유지한다. */
    @Test
    void preservesOriginalFileNameContentTypeAndBytes() throws Exception {
        byte[] bytes = {(byte) 0xff, (byte) 0xd8, (byte) 0xff, 1, 2, 3, 4};
        var file = new MockMultipartFile("logoFile", "folder/Original Name.JPG", "image/jpeg", bytes);
        var exhibition = new ExhibitionEntity(UUID.randomUUID(), "original-slug", "전시");
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0));
        when(minio.putObject(any())).thenAnswer(call -> {
            PutObjectArgs args = call.getArgument(0);
            assertThat(args.bucket()).isEqualTo("test");
            assertThat(args.object()).isEqualTo("original-slug/Original Name.JPG");
            assertThat(args.contentType()).isEqualTo("image/jpeg");
            assertThat(args.stream().readAllBytes()).isEqualTo(bytes);
            return null;
        });
        var asset = service.uploadForExhibition(exhibition, file, "image");
        assertThat(asset.getObjectKey()).isEqualTo("original-slug/Original Name.JPG");
        assertThat(asset.getSize()).isEqualTo(bytes.length);
    }

    private ItemEntity itemWithPeriod() {
        var exhibition = new ExhibitionEntity(UUID.randomUUID(), "slug", "전시");
        var period = new EventPeriodEntity(UUID.randomUUID(), exhibition, "2026", null, null);
        return new ItemEntity(UUID.randomUUID(), exhibition, new CategoryEntity(UUID.randomUUID(), exhibition, "AI"), period, "작품", null);
    }

    private MockMultipartFile image() {
        return new MockMultipartFile("mediaFiles", "same.jpg", "image/jpeg", new byte[]{(byte) 0xff, (byte) 0xd8, (byte) 0xff});
    }

    @Test
    void rejectsItemMediaWithoutEventPeriod() {
        ExhibitionEntity exhibition = new ExhibitionEntity(UUID.randomUUID(), "slug", "전시");
        ItemEntity item = new ItemEntity(UUID.randomUUID(), exhibition,
                new CategoryEntity(UUID.randomUUID(), exhibition, "AI"), null, "작품", null);
        MockMultipartFile file = new MockMultipartFile("thumbnailFile", "thumb.jpg", "image/jpeg",
                new byte[] {(byte) 0xff, (byte) 0xd8, (byte) 0xff});

        assertThatThrownBy(() -> service.uploadForItem(item, file, "image"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("event period");
    }

    @Test
    void rejectsVideoInImagePart() {
        ExhibitionEntity exhibition = new ExhibitionEntity(UUID.randomUUID(), "slug", "전시");
        MockMultipartFile file = new MockMultipartFile("logoFile", "logo.mp4", "video/mp4",
                new byte[] {0, 0, 0, 0, 'f', 't', 'y', 'p'});

        assertThatThrownBy(() -> service.uploadForExhibition(exhibition, file, "image"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("image");
    }
}
