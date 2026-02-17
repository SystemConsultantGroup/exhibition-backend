package kr.ac.skku.scg.exhibition.item.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import kr.ac.skku.scg.exhibition.category.domain.CategoryEntity;
import kr.ac.skku.scg.exhibition.category.repository.CategoryRepository;
import kr.ac.skku.scg.exhibition.classification.domain.ItemClassificationEntity;
import kr.ac.skku.scg.exhibition.classification.repository.ItemClassificationRepository;
import kr.ac.skku.scg.exhibition.eventperiod.domain.EventPeriodEntity;
import kr.ac.skku.scg.exhibition.eventperiod.repository.EventPeriodRepository;
import kr.ac.skku.scg.exhibition.exhibition.domain.ExhibitionEntity;
import kr.ac.skku.scg.exhibition.exhibition.repository.ExhibitionRepository;
import kr.ac.skku.scg.exhibition.global.auth.resolver.AuthenticatedUser;
import kr.ac.skku.scg.exhibition.global.error.ForbiddenException;
import kr.ac.skku.scg.exhibition.item.dto.request.ItemBulkTemplateRequest;
import kr.ac.skku.scg.exhibition.item.dto.response.ItemBulkTemplateFile;
import kr.ac.skku.scg.exhibition.user.domain.UserType;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ItemBulkTemplateServiceTest {

    @Mock
    private ExhibitionRepository exhibitionRepository;

    @Mock
    private EventPeriodRepository eventPeriodRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ItemClassificationRepository classificationRepository;

    private ItemBulkTemplateService itemBulkTemplateService;

    @BeforeEach
    void setUp() {
        itemBulkTemplateService = new ItemBulkTemplateService(
                exhibitionRepository,
                eventPeriodRepository,
                categoryRepository,
                classificationRepository
        );
    }

    @Test
    void generateTemplate_adminOnly() {
        ItemBulkTemplateRequest request = new ItemBulkTemplateRequest();
        request.setExhibitionId(UUID.randomUUID());
        request.setEventPeriodId(UUID.randomUUID());

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

        assertThatThrownBy(() -> itemBulkTemplateService.generateTemplate(request, nonAdmin))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Admin role is required");
    }

    @Test
    void generateTemplate_includesMetadataAndOptions() throws Exception {
        UUID exhibitionId = UUID.randomUUID();
        UUID eventPeriodId = UUID.randomUUID();
        ExhibitionEntity exhibition = new ExhibitionEntity(exhibitionId, "test-exhibition", "테스트 전시");
        EventPeriodEntity eventPeriod = new EventPeriodEntity(
                eventPeriodId,
                exhibition,
                "2026-1",
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-12-31T23:59:59Z")
        );

        when(exhibitionRepository.findById(exhibitionId)).thenReturn(Optional.of(exhibition));
        when(eventPeriodRepository.findByIdAndExhibition_Id(eventPeriodId, exhibitionId)).thenReturn(Optional.of(eventPeriod));
        when(categoryRepository.findAllByExhibition_Id(exhibitionId)).thenReturn(List.of(
                new CategoryEntity(UUID.randomUUID(), exhibition, "인터랙티브"),
                new CategoryEntity(UUID.randomUUID(), exhibition, "AI")
        ));
        when(classificationRepository.findAllByExhibition_Id(exhibitionId)).thenReturn(List.of(
                new ItemClassificationEntity(UUID.randomUUID(), exhibition, "작품"),
                new ItemClassificationEntity(UUID.randomUUID(), exhibition, "논문")
        ));

        ItemBulkTemplateRequest request = new ItemBulkTemplateRequest();
        request.setExhibitionId(exhibitionId);
        request.setEventPeriodId(eventPeriodId);

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

        ItemBulkTemplateFile file = itemBulkTemplateService.generateTemplate(request, admin);

        assertThat(file.fileName()).contains("item-bulk-template-");
        assertThat(file.bytes()).isNotEmpty();

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(file.bytes()))) {
            assertThat(workbook.getSheet("items_template")).isNotNull();
            assertThat(workbook.getSheet("options")).isNotNull();
            assertThat(workbook.getSheet("items_template").getRow(1).getCell(0).getStringCellValue())
                    .isEqualTo(exhibitionId.toString());
            assertThat(workbook.getSheet("items_template").getRow(1).getCell(1).getStringCellValue())
                    .isEqualTo(eventPeriodId.toString());
            assertThat(workbook.getSheet("items_template").getRow(1).getCell(2).getStringCellValue())
                    .isEqualTo("2026-1");
            assertThat(workbook.getSheet("options").getRow(4).getCell(0).getStringCellValue()).isEqualTo("AI");
            assertThat(workbook.getSheet("options").getRow(4).getCell(1).getStringCellValue()).isEqualTo("논문");
        }
    }
}
