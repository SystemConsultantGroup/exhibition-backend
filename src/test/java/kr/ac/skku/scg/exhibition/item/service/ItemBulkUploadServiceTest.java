package kr.ac.skku.scg.exhibition.item.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import kr.ac.skku.scg.exhibition.category.domain.CategoryEntity;
import kr.ac.skku.scg.exhibition.category.repository.CategoryRepository;
import kr.ac.skku.scg.exhibition.classification.domain.ItemClassificationEntity;
import kr.ac.skku.scg.exhibition.classification.repository.ItemClassificationMapRepository;
import kr.ac.skku.scg.exhibition.classification.repository.ItemClassificationRepository;
import kr.ac.skku.scg.exhibition.eventperiod.domain.EventPeriodEntity;
import kr.ac.skku.scg.exhibition.eventperiod.repository.EventPeriodRepository;
import kr.ac.skku.scg.exhibition.exhibition.domain.ExhibitionEntity;
import kr.ac.skku.scg.exhibition.exhibition.repository.ExhibitionRepository;
import kr.ac.skku.scg.exhibition.global.auth.resolver.AuthenticatedUser;
import kr.ac.skku.scg.exhibition.global.error.ForbiddenException;
import kr.ac.skku.scg.exhibition.item.domain.ItemEntity;
import kr.ac.skku.scg.exhibition.item.dto.response.ItemBulkUploadResponse;
import kr.ac.skku.scg.exhibition.item.repository.ItemRepository;
import kr.ac.skku.scg.exhibition.media.domain.MediaAssetEntity;
import kr.ac.skku.scg.exhibition.media.repository.MediaAssetRepository;
import kr.ac.skku.scg.exhibition.user.domain.UserType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class ItemBulkUploadServiceTest {

    @Mock
    private ExhibitionRepository exhibitionRepository;

    @Mock
    private EventPeriodRepository eventPeriodRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ItemClassificationRepository classificationRepository;

    @Mock
    private ItemClassificationMapRepository itemClassificationMapRepository;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private MediaAssetRepository mediaAssetRepository;

    private ItemBulkUploadService itemBulkUploadService;

    @BeforeEach
    void setUp() {
        itemBulkUploadService = new ItemBulkUploadService(
                exhibitionRepository,
                eventPeriodRepository,
                categoryRepository,
                classificationRepository,
                itemClassificationMapRepository,
                itemRepository,
                mediaAssetRepository
        );
    }

    @Test
    void upload_adminOnly() {
        MockMultipartFile file = new MockMultipartFile("file", "items.xlsx", "application/octet-stream", new byte[] {1});
        AuthenticatedUser nonAdmin = new AuthenticatedUser(
                UUID.randomUUID(),
                "visitor",
                UserType.VISITOR,
                null,
                null,
                null,
                null,
                true
        );

        assertThatThrownBy(() -> itemBulkUploadService.upload(file, nonAdmin))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Admin role is required");
    }

    @Test
    void upload_parsesRowsAndCreatesRelations() throws IOException {
        UUID exhibitionId = UUID.randomUUID();
        UUID eventPeriodId = UUID.randomUUID();

        ExhibitionEntity exhibition = new ExhibitionEntity(exhibitionId, "slug", "전시");
        EventPeriodEntity eventPeriod = new EventPeriodEntity(
                eventPeriodId,
                exhibition,
                "2026-1",
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-12-31T23:59:59Z")
        );
        CategoryEntity category = new CategoryEntity(UUID.randomUUID(), exhibition, "AI");
        ItemClassificationEntity classificationA = new ItemClassificationEntity(UUID.randomUUID(), exhibition, "작품");

        when(exhibitionRepository.findById(exhibitionId)).thenReturn(Optional.of(exhibition));
        when(eventPeriodRepository.findByIdAndExhibition_Id(eventPeriodId, exhibitionId)).thenReturn(Optional.of(eventPeriod));
        when(categoryRepository.findByExhibition_IdAndName(exhibitionId, "AI")).thenReturn(Optional.of(category));
        when(classificationRepository.findByExhibition_IdAndName(exhibitionId, "작품")).thenReturn(Optional.of(classificationA));
        when(itemRepository.save(any(ItemEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mediaAssetRepository.save(any(MediaAssetEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(itemClassificationMapRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "items.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                createWorkbookBytes(exhibitionId, eventPeriodId)
        );

        AuthenticatedUser admin = new AuthenticatedUser(
                UUID.randomUUID(),
                "admin",
                UserType.ADMIN,
                "admin@example.com",
                null,
                null,
                null,
                true
        );

        ItemBulkUploadResponse response = itemBulkUploadService.upload(file, admin);

        assertThat(response.createdItems()).isEqualTo(1);
        assertThat(response.createdMediaAssets()).isEqualTo(3);
        assertThat(response.createdClassificationMappings()).isEqualTo(1);
    }

    private byte[] createWorkbookBytes(UUID exhibitionId, UUID eventPeriodId) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("items_template");
            Row header = sheet.createRow(0);
            for (int i = 0; i <= 11; i++) {
                header.createCell(i).setCellValue("h" + i);
            }

            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue(exhibitionId.toString());
            row.createCell(1).setCellValue(eventPeriodId.toString());
            row.createCell(3).setCellValue("AI");
            row.createCell(4).setCellValue("작품");
            row.createCell(5).setCellValue("Smart Campus");
            row.createCell(6).setCellValue("desc");
            row.createCell(7).setCellValue("홍길동,양현준");
            row.createCell(8).setCellValue("김교수");
            row.createCell(9).setCellValue("items/1/thumb.jpg");
            row.createCell(10).setCellValue("items/1/poster.png");
            row.createCell(11).setCellValue("items/1/video.mp4");

            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }
}
