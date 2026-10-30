package kr.ac.skku.scg.exhibition.media.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import java.time.Instant;
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
        assertThat(media.getObjectKey()).startsWith("slug/").endsWith("/logo.png");
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

        assertThat(media.getObjectKey()).startsWith("slug/" + period.getId() + "/").endsWith("/thumb.jpg");
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
