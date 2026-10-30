package kr.ac.skku.scg.exhibition.item.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import jakarta.persistence.EntityManager;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import kr.ac.skku.scg.exhibition.category.domain.CategoryEntity;
import kr.ac.skku.scg.exhibition.category.repository.CategoryRepository;
import kr.ac.skku.scg.exhibition.classification.domain.ItemClassificationEntity;
import kr.ac.skku.scg.exhibition.classification.repository.ItemClassificationRepository;
import kr.ac.skku.scg.exhibition.classification.repository.ItemClassificationMapRepository;
import kr.ac.skku.scg.exhibition.eventperiod.domain.EventPeriodEntity;
import kr.ac.skku.scg.exhibition.eventperiod.repository.EventPeriodRepository;
import kr.ac.skku.scg.exhibition.exhibition.domain.ExhibitionEntity;
import kr.ac.skku.scg.exhibition.exhibition.repository.ExhibitionRepository;
import kr.ac.skku.scg.exhibition.global.auth.resolver.AuthenticatedUser;
import kr.ac.skku.scg.exhibition.global.auth.service.AdminAuthorizationService;
import kr.ac.skku.scg.exhibition.global.config.MinioProperties;
import kr.ac.skku.scg.exhibition.item.repository.ItemRepository;
import kr.ac.skku.scg.exhibition.media.repository.MediaAssetRepository;
import kr.ac.skku.scg.exhibition.media.service.MediaService;
import kr.ac.skku.scg.exhibition.user.domain.UserType;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.transaction.TestTransaction;

@DataJpaTest(properties = {"spring.jpa.properties.hibernate.jdbc.batch_size=50",
        "spring.jpa.properties.hibernate.order_inserts=true", "spring.jpa.properties.hibernate.order_updates=true"})
@Import({ItemBulkUploadService.class, MediaService.class, ItemBulkUploadIntegrationTest.Config.class})
class ItemBulkUploadIntegrationTest {
    @TestConfiguration
    static class Config {
        @Bean MinioProperties minioProperties() {
            var properties = new MinioProperties();
            properties.setBucket("test");
            return properties;
        }
        @Bean Executor mediaUploadExecutor() { return Runnable::run; }
    }

    @Autowired ItemBulkUploadService service;
    @Autowired ExhibitionRepository exhibitions;
    @Autowired EventPeriodRepository periods;
    @Autowired CategoryRepository categories;
    @Autowired ItemClassificationRepository classifications;
    @Autowired ItemClassificationMapRepository mappings;
    @Autowired ItemRepository items;
    @Autowired MediaAssetRepository media;
    @Autowired EntityManager entityManager;
    @MockitoBean MinioClient minio;
    @MockitoBean AdminAuthorizationService authorization;

    /** 실제 H2/JPA 배치에서 순환 미디어 FK가 저장되며 트랜잭션 롤백은 객체까지 정리한다. */
    @Test
    void bulkPersistAndRollbackKeepDatabaseAndStorageConsistent() throws Exception {
        var exhibition = exhibitions.save(new ExhibitionEntity(null, "integration", "통합 테스트"));
        var period = periods.save(new EventPeriodEntity(null, exhibition, "2026", Instant.now(), Instant.now().plusSeconds(60)));
        categories.save(new CategoryEntity(null, exhibition, "AI"));
        classifications.save(new ItemClassificationEntity(null, exhibition, "작품"));
        byte[] workbookBytes;
        try (var workbook = new XSSFWorkbook(); var output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("items_template");
            var header = sheet.createRow(0);
            for (int i = 0; i < ItemBulkTemplateService.TEMPLATE_HEADERS.size(); i++) {
                header.createCell(i).setCellValue(ItemBulkTemplateService.TEMPLATE_HEADERS.get(i));
            }
            var options = workbook.createSheet("options");
            var optionHeader = options.createRow(0);
            optionHeader.createCell(0).setCellValue("exhibition_id");
            optionHeader.createCell(1).setCellValue("event_period_id");
            optionHeader.createCell(2).setCellValue("event_period_name");
            var metadata = options.createRow(1);
            metadata.createCell(0).setCellValue(exhibition.getId().toString());
            metadata.createCell(1).setCellValue(period.getId().toString());
            for (int i = 1; i <= 30; i++) {
                var row = sheet.createRow(i);
                row.createCell(0).setCellValue("AI");
                row.createCell(1).setCellValue("작품");
                row.createCell(2).setCellValue("작품 " + i);
                row.createCell(8).setCellValue("same.jpg");
            }
            workbook.write(output);
            workbookBytes = output.toByteArray();
        }
        var file = new MockMultipartFile("file", "items.xlsx", "application/octet-stream", workbookBytes);
        var image = new MockMultipartFile("mediaFiles", "same.jpg", "image/jpeg",
                new byte[]{(byte) 0xff, (byte) 0xd8, (byte) 0xff});
        var user = new AuthenticatedUser(UUID.randomUUID(), "admin", UserType.ADMIN, null, null, null, null, true);
        var result = service.upload(file, List.of(image), exhibition.getId(), user);
        entityManager.flush();
        entityManager.clear();
        assertThat(result.createdItems()).isEqualTo(30);
        assertThat(items.count()).isEqualTo(30);
        assertThat(media.count()).isEqualTo(30);
        assertThat(mappings.count()).isEqualTo(30);
        assertThat(items.findAll()).allSatisfy(item -> {
            assertThat(item.getThumbnailMedia()).isNotNull();
            assertThat(item.getThumbnailMedia().getItem().getId()).isEqualTo(item.getId());
            assertThat(item.getThumbnailMedia().getObjectKey())
                    .isEqualTo("integration/" + period.getId() + "/same.jpg");
        });
        TestTransaction.flagForRollback();
        TestTransaction.end();
        verify(minio).putObject(any(io.minio.PutObjectArgs.class));
        verify(minio).removeObject(any(RemoveObjectArgs.class));
        TestTransaction.start();
        assertThat(items.count()).isZero();
        assertThat(media.count()).isZero();
        assertThat(mappings.count()).isZero();
    }
}
