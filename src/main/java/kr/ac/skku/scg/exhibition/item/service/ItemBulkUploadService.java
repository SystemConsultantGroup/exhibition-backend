package kr.ac.skku.scg.exhibition.item.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import kr.ac.skku.scg.exhibition.category.domain.CategoryEntity;
import kr.ac.skku.scg.exhibition.category.repository.CategoryRepository;
import kr.ac.skku.scg.exhibition.classification.domain.ItemClassificationEntity;
import kr.ac.skku.scg.exhibition.classification.domain.ItemClassificationMapEntity;
import kr.ac.skku.scg.exhibition.classification.repository.ItemClassificationMapRepository;
import kr.ac.skku.scg.exhibition.classification.repository.ItemClassificationRepository;
import kr.ac.skku.scg.exhibition.eventperiod.domain.EventPeriodEntity;
import kr.ac.skku.scg.exhibition.eventperiod.repository.EventPeriodRepository;
import kr.ac.skku.scg.exhibition.exhibition.domain.ExhibitionEntity;
import kr.ac.skku.scg.exhibition.exhibition.repository.ExhibitionRepository;
import kr.ac.skku.scg.exhibition.global.auth.resolver.AuthenticatedUser;
import kr.ac.skku.scg.exhibition.global.auth.service.AdminAuthorizationService;
import kr.ac.skku.scg.exhibition.global.error.NotFoundException;
import kr.ac.skku.scg.exhibition.item.domain.ItemEntity;
import kr.ac.skku.scg.exhibition.item.dto.response.ItemBulkUploadResponse;
import kr.ac.skku.scg.exhibition.item.repository.ItemRepository;
import kr.ac.skku.scg.exhibition.media.domain.MediaAssetEntity;
import kr.ac.skku.scg.exhibition.media.service.MediaService;
import kr.ac.skku.scg.exhibition.media.service.MediaService.ItemMediaUpload;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ItemBulkUploadService {
    private static final String TEMPLATE_SHEET = "items_template";
    private static final String OPTIONS_SHEET = "options";
    private static final int MAX_ROWS = 1000;
    private static final long MAX_EXCEL_BYTES = 10L * 1024 * 1024;

    private final ExhibitionRepository exhibitions;
    private final EventPeriodRepository periods;
    private final CategoryRepository categories;
    private final ItemClassificationRepository classifications;
    private final ItemClassificationMapRepository mappings;
    private final ItemRepository items;
    private final MediaService mediaService;
    private final AdminAuthorizationService authorization;

    public ItemBulkUploadService(ExhibitionRepository exhibitions, EventPeriodRepository periods,
            CategoryRepository categories, ItemClassificationRepository classifications,
            ItemClassificationMapRepository mappings, ItemRepository items, MediaService mediaService,
            AdminAuthorizationService authorization) {
        this.exhibitions = exhibitions;
        this.periods = periods;
        this.categories = categories;
        this.classifications = classifications;
        this.mappings = mappings;
        this.items = items;
        this.mediaService = mediaService;
        this.authorization = authorization;
    }

    /** XLSX와 함께 전달된 미디어를 한 트랜잭션으로 등록한다. */
    @Transactional
    public ItemBulkUploadResponse upload(MultipartFile file, List<MultipartFile> mediaFiles,
            UUID currentExhibitionId, AuthenticatedUser user) {
        authorization.requireExhibitionAdmin(user, currentExhibitionId);
        validateFile(file);
        Map<String, MultipartFile> files = indexMediaFiles(mediaFiles);
        Set<String> used = new HashSet<>();
        List<PendingItem> pending = new ArrayList<>();
        Map<String, CategoryEntity> categoryCache = new HashMap<>();
        Map<String, ItemClassificationEntity> classificationCache = new HashMap<>();
        Set<String> validatedMedia = new HashSet<>();
        DataFormatter formatter = new DataFormatter();

        try (InputStream input = file.getInputStream(); Workbook workbook = new XSSFWorkbook(input)) {
            Sheet sheet = workbook.getSheet(TEMPLATE_SHEET);
            Sheet options = workbook.getSheet(OPTIONS_SHEET);
            validateWorkbook(sheet, options, formatter);
            UUID metadataExhibitionId = parseUuid(cellValue(options.getRow(1), 0, formatter), "metadata exhibition_id", 2);
            if (!currentExhibitionId.equals(metadataExhibitionId)) {
                throw new IllegalArgumentException("Workbook exhibition does not match current exhibition");
            }
            UUID eventPeriodId = parseUuid(cellValue(options.getRow(1), 1, formatter), "metadata event_period_id", 2);
            ExhibitionEntity exhibition = exhibitions.findById(currentExhibitionId)
                    .orElseThrow(() -> new NotFoundException("Exhibition not found: " + currentExhibitionId));
            EventPeriodEntity period = periods.findByIdAndExhibition_Id(eventPeriodId, currentExhibitionId)
                    .orElseThrow(() -> new NotFoundException("Event period not found: " + eventPeriodId));

            int populatedRows = 0;
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (isDataRowEmpty(row, formatter)) {
                    continue;
                }
                if (++populatedRows > MAX_ROWS) {
                    throw new IllegalArgumentException("At most 1000 item rows are allowed");
                }
                int rowNumber = rowIndex + 1;
                String categoryName = required(cellValue(row, 0, formatter), "category_name", rowNumber);
                String title = required(cellValue(row, 2, formatter), "title", rowNumber);
                if (title.length() > 200) {
                    throw new IllegalArgumentException(
                            "Row " + rowNumber + ": title must be at most 200 characters");
                }
                CategoryEntity category = categoryCache.computeIfAbsent(categoryName, name ->
                        categories.findByExhibition_IdAndName(currentExhibitionId, name)
                                .orElseThrow(() -> new NotFoundException("Row " + rowNumber + ": category not found: " + name)));
                ItemEntity item = new ItemEntity(null, exhibition, category, period, title, nullable(cellValue(row, 3, formatter)));
                item.updateMetadata(category, period, title, nullable(cellValue(row, 3, formatter)),
                        nullable(cellValue(row, 4, formatter)), nullable(cellValue(row, 5, formatter)),
                        nullable(cellValue(row, 6, formatter)), parseAwarded(cellValue(row, 7, formatter), rowNumber));
                MultipartFile thumbnail = reference(cellValue(row, 8, formatter), "thumbnail_file_name",
                        rowNumber, "image", files, used, validatedMedia);
                MultipartFile poster = reference(cellValue(row, 9, formatter), "poster_file_name",
                        rowNumber, "image", files, used, validatedMedia);
                MultipartFile video = reference(cellValue(row, 10, formatter), "presentation_video_file_name",
                        rowNumber, "video", files, used, validatedMedia);
                String classificationName = nullable(cellValue(row, 1, formatter));
                ItemClassificationEntity classification = classificationName == null ? null
                        : classificationCache.computeIfAbsent(classificationName, name ->
                                classifications.findByExhibition_IdAndName(currentExhibitionId, name)
                                        .orElseThrow(() -> new NotFoundException(
                                                "Row " + rowNumber + ": classification not found: " + name)));
                pending.add(new PendingItem(item, classification, thumbnail, poster, video));
            }
            Set<String> unused = new HashSet<>(files.keySet());
            unused.removeAll(used);
            if (!unused.isEmpty()) {
                throw new IllegalArgumentException("Unused mediaFiles: " + unused);
            }
        } catch (IOException ex) {
            throw new IllegalArgumentException("Failed to read excel file", ex);
        }
        // 모든 셀/참조/시그니처 검증이 끝난 뒤에만 DB와 MinIO에 부작용을 발생시킨다.
        items.saveAll(pending.stream().map(PendingItem::item).toList());
        List<ItemMediaUpload> uploads = new ArrayList<>();
        for (PendingItem row : pending) {
            if (row.thumbnail() != null) uploads.add(new ItemMediaUpload(row.item(), row.thumbnail(), "image"));
            if (row.poster() != null) uploads.add(new ItemMediaUpload(row.item(), row.poster(), "image"));
            if (row.video() != null) uploads.add(new ItemMediaUpload(row.item(), row.video(), "video"));
        }
        List<MediaAssetEntity> assets = uploads.isEmpty() ? List.of() : mediaService.uploadForItems(uploads);
        int index = 0;
        List<ItemClassificationMapEntity> classificationMappings = new ArrayList<>();
        for (PendingItem row : pending) {
            row.item().updateMedia(row.thumbnail() == null ? null : assets.get(index++),
                    row.poster() == null ? null : assets.get(index++), row.video() == null ? null : assets.get(index++));
            if (row.classification() != null) {
                classificationMappings.add(new ItemClassificationMapEntity(null, row.item(), row.classification()));
            }
        }
        mappings.saveAll(classificationMappings);
        return new ItemBulkUploadResponse(pending.size(), assets.size(), classificationMappings.size());
    }

    private record PendingItem(ItemEntity item, ItemClassificationEntity classification,
            MultipartFile thumbnail, MultipartFile poster, MultipartFile video) { }

    /** 템플릿 시트와 숨은 메타데이터 및 헤더를 검증한다. */
    private void validateWorkbook(Sheet sheet, Sheet options, DataFormatter formatter) {
        if (sheet == null || options == null || options.getRow(0) == null || options.getRow(1) == null) {
            throw new IllegalArgumentException("Invalid item bulk template");
        }
        Row header = sheet.getRow(0);
        for (int i = 0; i < ItemBulkTemplateService.TEMPLATE_HEADERS.size(); i++) {
            String expectedHeader = ItemBulkTemplateService.TEMPLATE_HEADERS.get(i);
            if (!expectedHeader.equals(cellValue(header, i, formatter))) {
                throw new IllegalArgumentException("Invalid template header: " + expectedHeader);
            }
        }
        String[] metadataHeaders = {"exhibition_id", "event_period_id", "event_period_name"};
        for (int i = 0; i < metadataHeaders.length; i++) {
            if (!metadataHeaders[i].equals(cellValue(options.getRow(0), i, formatter))) {
                throw new IllegalArgumentException("Invalid template metadata header: " + metadataHeaders[i]);
            }
        }
    }

    /** 파일명 참조 및 타입별 시그니처를 저장 전에 한 번만 검증한다. */
    private MultipartFile reference(String raw, String column, int row, String expectedType,
            Map<String, MultipartFile> files, Set<String> used, Set<String> validated) {
        String reference = nullable(raw);
        if (reference == null) {
            return null;
        }
        String name = fileName(reference);
        MultipartFile file = files.get(name);
        if (file == null) {
            throw new IllegalArgumentException(
                    "Row " + row + ": " + column + " has no matching mediaFiles file: " + name);
        }
        used.add(name);
        try {
            if (validated.add(expectedType + ":" + name)) mediaService.validateUpload(file, expectedType);
            return file;
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "Row " + row + ": invalid media type or signature for " + column + ": " + name
                            + " (" + ex.getMessage() + ")",
                    ex);
        }
    }

    /** multipart 미디어를 파일명으로 색인한다. */
    private Map<String, MultipartFile> indexMediaFiles(List<MultipartFile> mediaFiles) {
        Map<String, MultipartFile> result = new HashMap<>();
        if (mediaFiles == null) {
            return result;
        }
        for (MultipartFile file : mediaFiles) {
            if (file == null || file.isEmpty()) throw new IllegalArgumentException("mediaFiles must not contain empty files");
            String name = fileName(file.getOriginalFilename());
            if (name.isBlank()) throw new IllegalArgumentException("Every media file must have a file name");
            if (result.putIfAbsent(name, file) != null) throw new IllegalArgumentException("Duplicate media file name: " + name);
        }
        return result;
    }

    /** 경로 입력에서 안전한 파일명만 추출한다. */
    private String fileName(String raw) {
        if (raw == null) return "";
        String value = raw.trim().replace('\\', '/'); return value.substring(value.lastIndexOf('/') + 1);
    }

    /** Y/N 수상 여부를 읽는다. */
    private boolean parseAwarded(String raw, int row) {
        String value = nullable(raw); if (value == null || "N".equalsIgnoreCase(value)) return false;
        if ("Y".equalsIgnoreCase(value)) return true;
        throw new IllegalArgumentException("Row " + row + ": awarded_y_n must be Y or N");
    }

    /** 사용자 입력 행이 비어 있는지 확인한다. */
    private boolean isDataRowEmpty(Row row, DataFormatter formatter) {
        if (row == null) return true;
        for (int i = 0; i < ItemBulkTemplateService.TEMPLATE_HEADERS.size(); i++) {
            if (!cellValue(row, i, formatter).isBlank()) {
                return false;
            }
        }
        return true;
    }

    /** 필수 셀 값을 읽는다. */
    private String required(String value, String column, int row) {
        String normalized = nullable(value); if (normalized == null) throw new IllegalArgumentException("Row " + row + ": " + column + " is required");
        return normalized;
    }

    /** UUID 셀 값을 읽는다. */
    private UUID parseUuid(String value, String column, int row) {
        try { return UUID.fromString(required(value, column, row)); }
        catch (IllegalArgumentException ex) { throw new IllegalArgumentException("Row " + row + ": invalid UUID in " + column); }
    }

    /** 빈 문자열을 null로 정규화한다. */
    private String nullable(String value) { if (value == null) return null; String v = value.trim(); return v.isEmpty() ? null : v; }

    /** 셀 표시값을 읽는다. */
    private String cellValue(Row row, int index, DataFormatter formatter) {
        return row == null || row.getCell(index) == null ? "" : formatter.formatCellValue(row.getCell(index));
    }

    /** XLSX 파일 존재 여부를 검증한다. */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Excel file is required");
        }
        if (file.getSize() > MAX_EXCEL_BYTES) {
            throw new IllegalArgumentException("Excel file must be at most 10 MiB");
        }
    }
}
