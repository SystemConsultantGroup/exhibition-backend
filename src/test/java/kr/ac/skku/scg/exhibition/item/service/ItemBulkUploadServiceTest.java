package kr.ac.skku.scg.exhibition.item.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.minio.MinioClient;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
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
import kr.ac.skku.scg.exhibition.global.auth.service.AdminAuthorizationService;
import kr.ac.skku.scg.exhibition.global.config.MinioProperties;
import kr.ac.skku.scg.exhibition.global.error.ForbiddenException;
import kr.ac.skku.scg.exhibition.item.domain.ItemEntity;
import kr.ac.skku.scg.exhibition.item.dto.response.ItemBulkUploadResponse;
import kr.ac.skku.scg.exhibition.item.repository.ItemRepository;
import kr.ac.skku.scg.exhibition.media.domain.MediaAssetEntity;
import kr.ac.skku.scg.exhibition.media.repository.MediaAssetRepository;
import kr.ac.skku.scg.exhibition.media.service.MediaService;
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
import org.springframework.web.multipart.MultipartFile;

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

    @Mock
    private MinioClient minioClient;

    @Mock
    private AdminAuthorizationService adminAuthorizationService;

    @Mock
    private MediaService mediaService;

    private ItemBulkUploadService itemBulkUploadService;

    @BeforeEach
    void setUp() {
        MinioProperties minioProperties = new MinioProperties();
        minioProperties.setBucket("exhibition");
        itemBulkUploadService = new ItemBulkUploadService(
                exhibitionRepository,
                eventPeriodRepository,
                categoryRepository,
                classificationRepository,
                itemClassificationMapRepository,
                itemRepository,
                mediaService,
                adminAuthorizationService
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

        UUID exhibitionId = UUID.randomUUID();
        org.mockito.Mockito.doThrow(new ForbiddenException("Admin role is required"))
                .when(adminAuthorizationService).requireExhibitionAdmin(nonAdmin, exhibitionId);
        assertThatThrownBy(() -> itemBulkUploadService.upload(file, List.of(), exhibitionId, nonAdmin))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Admin role is required");
    }

    @Test
    void upload_rejectsExcelLargerThanTenMiB() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(10L * 1024 * 1024 + 1);

        assertThatThrownBy(() -> itemBulkUploadService.upload(
                file, List.of(), UUID.randomUUID(), user(UserType.ADMIN)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("10 MiB");
    }

    @Test
    void upload_rejectsWorkbookFromAnotherExhibition() throws Exception {
        UUID currentExhibitionId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "items.xlsx", "application/octet-stream",
                createWorkbookBytes(UUID.randomUUID(), UUID.randomUUID()));

        assertThatThrownBy(() -> itemBulkUploadService.upload(
                file, List.of(), currentExhibitionId, user(UserType.SUB_ADMIN)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not match");
    }

    @Test
    void upload_rejectsBadHeader() throws Exception {
        UUID exhibitionId = UUID.randomUUID();
        byte[] bytes = createWorkbookBytes(exhibitionId, UUID.randomUUID(), "wrong_header");

        assertThatThrownBy(() -> itemBulkUploadService.upload(
                new MockMultipartFile("file", "items.xlsx", "application/octet-stream", bytes),
                List.of(), exhibitionId, user(UserType.ADMIN)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid template header");
    }

    @Test
    void upload_rejectsMissingReferencedMedia() throws Exception {
        UUID exhibitionId = UUID.randomUUID();
        UUID eventPeriodId = UUID.randomUUID();
        stubWorkbookContext(exhibitionId, eventPeriodId);

        assertThatThrownBy(() -> itemBulkUploadService.upload(
                new MockMultipartFile("file", "items.xlsx", "application/octet-stream",
                        createWorkbookBytes(exhibitionId, eventPeriodId)),
                List.of(), exhibitionId, user(UserType.ADMIN)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no matching mediaFiles file");
    }

    @Test
    void upload_rejectsVideoInImageColumn() throws Exception {
        UUID exhibitionId = UUID.randomUUID();
        UUID eventPeriodId = UUID.randomUUID();
        stubWorkbookContext(exhibitionId, eventPeriodId);
        List<MultipartFile> files = List.of(
                new MockMultipartFile("mediaFiles", "thumb.jpg", "image/jpeg", new byte[] {1}),
                new MockMultipartFile("mediaFiles", "poster.png", "image/png", new byte[] {1}),
                new MockMultipartFile("mediaFiles", "video.mp4", "video/mp4", new byte[] {1}));
        byte[] bytes = createWorkbookBytes(exhibitionId, eventPeriodId, "category_name", "video.mp4");
        org.mockito.Mockito.doThrow(new IllegalArgumentException("Media must be an image file"))
                .when(mediaService).validateUpload(org.mockito.ArgumentMatchers.same(files.get(2)),
                        org.mockito.ArgumentMatchers.eq("image"));

        assertThatThrownBy(() -> itemBulkUploadService.upload(
                new MockMultipartFile("file", "items.xlsx", "application/octet-stream", bytes),
                files, exhibitionId, user(UserType.ADMIN)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid media type");
    }

    @Test
    void upload_parsesRowsAndUploadsMediaFiles() throws Exception {
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
        when(mediaService.uploadForItems(any())).thenAnswer(invocation -> {
            List<MediaService.ItemMediaUpload> uploads = invocation.getArgument(0);
            return uploads.stream().map(upload -> new MediaAssetEntity(null, upload.item(), exhibition,
                    "unique/" + upload.file().getOriginalFilename(), upload.file().getContentType(), upload.file().getSize())).toList();
        });

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

        List<MultipartFile> mediaFiles = List.of(
                new MockMultipartFile("mediaFiles", "thumb.jpg", "image/jpeg",
                        new byte[] {(byte) 0xff, (byte) 0xd8, (byte) 0xff}),
                new MockMultipartFile("mediaFiles", "poster.png", "image/png",
                        new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a}),
                new MockMultipartFile("mediaFiles", "video.mp4", "video/mp4",
                        new byte[] {0, 0, 0, 8, 'f', 't', 'y', 'p'})
        );

        ItemBulkUploadResponse response = itemBulkUploadService.upload(file, mediaFiles, exhibitionId, admin);

        assertThat(response.createdItems()).isEqualTo(1);
        assertThat(response.createdMediaAssets()).isEqualTo(3);
        assertThat(response.createdClassificationMappings()).isEqualTo(1);
        verify(mediaService).uploadForItems(org.mockito.ArgumentMatchers.argThat(uploads -> uploads.size() == 3));
        verify(mediaService, times(3)).validateUpload(any(MultipartFile.class), any());
        verify(itemRepository).saveAll(any());
        verify(itemClassificationMapRepository).saveAll(any());
    }

    @Test
    void upload_rejectsExtensionSignatureMismatch() throws Exception {
        UUID exhibitionId = UUID.randomUUID();
        UUID eventPeriodId = UUID.randomUUID();
        stubWorkbookContext(exhibitionId, eventPeriodId);
        List<MultipartFile> files = List.of(
                new MockMultipartFile("mediaFiles", "thumb.jpg", "image/jpeg", new byte[] {1, 2, 3}),
                new MockMultipartFile("mediaFiles", "poster.png", "image/png",
                        new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a}),
                new MockMultipartFile("mediaFiles", "video.mp4", "video/mp4",
                        new byte[] {0, 0, 0, 8, 'f', 't', 'y', 'p'}));
        org.mockito.Mockito.doThrow(new IllegalArgumentException("Media file signature does not match its extension"))
                .when(mediaService).validateUpload(org.mockito.ArgumentMatchers.same(files.get(0)),
                        org.mockito.ArgumentMatchers.eq("image"));

        assertThatThrownBy(() -> itemBulkUploadService.upload(
                new MockMultipartFile("file", "items.xlsx", "application/octet-stream",
                        createWorkbookBytes(exhibitionId, eventPeriodId)),
                files, exhibitionId, user(UserType.ADMIN)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("signature does not match its extension");
    }

    /** 같은 카테고리/분류를 100번 사용해도 조회는 각각 한 번이며 저장은 묶어서 한다. */
    @Test
    void upload_cachesRelationsAndBatchesOneHundredRows() throws Exception {
        UUID exhibitionId = UUID.randomUUID();
        UUID periodId = UUID.randomUUID();
        stubWorkbookContext(exhibitionId, periodId);
        ExhibitionEntity exhibition = new ExhibitionEntity(exhibitionId, "slug", "전시");
        when(classificationRepository.findByExhibition_IdAndName(exhibitionId, "작품"))
                .thenReturn(Optional.of(new ItemClassificationEntity(UUID.randomUUID(), exhibition, "작품")));
        byte[] bytes;
        try (Workbook workbook = new XSSFWorkbook(new java.io.ByteArrayInputStream(createWorkbookBytes(exhibitionId, periodId)));
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.getSheet("items_template");
            for (int i = 1; i <= 100; i++) {
                Row row = i == 1 ? sheet.getRow(1) : sheet.createRow(i);
                row.createCell(0).setCellValue("AI");
                row.createCell(1).setCellValue("작품");
                row.createCell(2).setCellValue("작품 " + i);
                row.createCell(8).setCellValue("");
                row.createCell(9).setCellValue("");
                row.createCell(10).setCellValue("");
            }
            workbook.write(output);
            bytes = output.toByteArray();
        }
        var result = itemBulkUploadService.upload(new MockMultipartFile("file", "items.xlsx", "application/octet-stream", bytes),
                List.of(), exhibitionId, user(UserType.ADMIN));
        assertThat(result.createdItems()).isEqualTo(100);
        assertThat(result.createdClassificationMappings()).isEqualTo(100);
        verify(categoryRepository).findByExhibition_IdAndName(exhibitionId, "AI");
        verify(classificationRepository).findByExhibition_IdAndName(exhibitionId, "작품");
        verify(itemRepository).saveAll(org.mockito.ArgumentMatchers.argThat(rows -> ((List<?>) rows).size() == 100));
        org.mockito.Mockito.verifyNoInteractions(mediaAssetRepository, minioClient);
    }

    /** 늦게 발견된 미사용 파일도 DB/MinIO 저장 전에 거부한다. */
    @Test
    void upload_rejectsUnusedFilesBeforeAnyWrites() throws Exception {
        UUID exhibitionId = UUID.randomUUID();
        UUID periodId = UUID.randomUUID();
        stubWorkbookContext(exhibitionId, periodId);
        ExhibitionEntity exhibition = new ExhibitionEntity(exhibitionId, "slug", "전시");
        when(classificationRepository.findByExhibition_IdAndName(exhibitionId, "작품"))
                .thenReturn(Optional.of(new ItemClassificationEntity(UUID.randomUUID(), exhibition, "작품")));
        var files = List.<MultipartFile>of(
                new MockMultipartFile("mediaFiles", "thumb.jpg", "image/jpeg", new byte[]{1}),
                new MockMultipartFile("mediaFiles", "poster.png", "image/png", new byte[]{1}),
                new MockMultipartFile("mediaFiles", "video.mp4", "video/mp4", new byte[]{1}),
                new MockMultipartFile("mediaFiles", "unused.png", "image/png", new byte[]{1}));
        assertThatThrownBy(() -> itemBulkUploadService.upload(new MockMultipartFile("file", "items.xlsx", "application/octet-stream",
                createWorkbookBytes(exhibitionId, periodId)), files, exhibitionId, user(UserType.ADMIN)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Unused mediaFiles");
        org.mockito.Mockito.verifyNoInteractions(itemRepository, itemClassificationMapRepository);
        verify(mediaService, org.mockito.Mockito.never()).uploadForItems(any());
    }

    private void stubWorkbookContext(UUID exhibitionId, UUID eventPeriodId) {
        ExhibitionEntity exhibition = new ExhibitionEntity(exhibitionId, "slug", "전시");
        EventPeriodEntity period = new EventPeriodEntity(eventPeriodId, exhibition, "2026",
                Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-12-31T00:00:00Z"));
        CategoryEntity category = new CategoryEntity(UUID.randomUUID(), exhibition, "AI");
        when(exhibitionRepository.findById(exhibitionId)).thenReturn(Optional.of(exhibition));
        when(eventPeriodRepository.findByIdAndExhibition_Id(eventPeriodId, exhibitionId)).thenReturn(Optional.of(period));
        when(categoryRepository.findByExhibition_IdAndName(exhibitionId, "AI")).thenReturn(Optional.of(category));
    }

    private AuthenticatedUser user(UserType role) {
        return new AuthenticatedUser(UUID.randomUUID(), "user", role, null, null, null, null, true);
    }

    private byte[] createWorkbookBytes(UUID exhibitionId, UUID eventPeriodId) throws IOException {
        return createWorkbookBytes(exhibitionId, eventPeriodId, "category_name");
    }

    private byte[] createWorkbookBytes(UUID exhibitionId, UUID eventPeriodId, String firstHeader) throws IOException {
        return createWorkbookBytes(exhibitionId, eventPeriodId, firstHeader, "thumb.jpg");
    }

    private byte[] createWorkbookBytes(UUID exhibitionId, UUID eventPeriodId, String firstHeader,
            String thumbnailFileName) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("items_template");
            String[] headers = {firstHeader, "classification_name", "title", "description",
                    "participant_names_csv", "participant_emails_csv", "advisor_names_csv", "awarded_y_n",
                    "thumbnail_file_name", "poster_file_name", "presentation_video_file_name"};
            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) header.createCell(i).setCellValue(headers[i]);
            Sheet options = workbook.createSheet("options");
            Row metadataHeader = options.createRow(0);
            metadataHeader.createCell(0).setCellValue("exhibition_id");
            metadataHeader.createCell(1).setCellValue("event_period_id");
            metadataHeader.createCell(2).setCellValue("event_period_name");
            Row metadata = options.createRow(1);
            metadata.createCell(0).setCellValue(exhibitionId.toString());
            metadata.createCell(1).setCellValue(eventPeriodId.toString());
            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue("AI");
            row.createCell(1).setCellValue("작품");
            row.createCell(2).setCellValue("Smart Campus");
            row.createCell(3).setCellValue("desc");
            row.createCell(4).setCellValue("홍길동,양현준");
            row.createCell(5).setCellValue("a@example.com");
            row.createCell(6).setCellValue("김교수");
            row.createCell(7).setCellValue("Y");
            row.createCell(8).setCellValue(thumbnailFileName);
            row.createCell(9).setCellValue("poster.png");
            row.createCell(10).setCellValue("video.mp4");
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }
}
