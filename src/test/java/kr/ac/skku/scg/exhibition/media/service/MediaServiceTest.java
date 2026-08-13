package kr.ac.skku.scg.exhibition.media.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.minio.MinioClient;
import java.util.UUID;
import kr.ac.skku.scg.exhibition.exhibition.domain.ExhibitionEntity;
import kr.ac.skku.scg.exhibition.global.config.MinioProperties;
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

        MediaAssetEntity media = service.uploadForExhibition(exhibition, file, "logo", "image");

        assertThat(media.getMediaType()).isEqualTo("image/png");
        assertThat(media.getExhibition()).isSameAs(exhibition);
        verify(minio).putObject(any());
    }

    @Test
    void rejectsVideoInImagePart() {
        ExhibitionEntity exhibition = new ExhibitionEntity(UUID.randomUUID(), "slug", "전시");
        MockMultipartFile file = new MockMultipartFile("logoFile", "logo.mp4", "video/mp4",
                new byte[] {0, 0, 0, 0, 'f', 't', 'y', 'p'});

        assertThatThrownBy(() -> service.uploadForExhibition(exhibition, file, "logo", "image"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("image");
    }
}
