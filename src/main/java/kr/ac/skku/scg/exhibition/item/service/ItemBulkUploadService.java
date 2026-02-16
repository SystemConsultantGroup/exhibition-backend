package kr.ac.skku.scg.exhibition.item.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
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
import kr.ac.skku.scg.exhibition.global.error.ForbiddenException;
import kr.ac.skku.scg.exhibition.global.error.NotFoundException;
import kr.ac.skku.scg.exhibition.item.domain.ItemEntity;
import kr.ac.skku.scg.exhibition.item.dto.response.ItemBulkUploadResponse;
import kr.ac.skku.scg.exhibition.item.repository.ItemRepository;
import kr.ac.skku.scg.exhibition.media.domain.MediaAssetEntity;
import kr.ac.skku.scg.exhibition.media.repository.MediaAssetRepository;
import kr.ac.skku.scg.exhibition.user.domain.UserType;
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

    private final ExhibitionRepository exhibitionRepository;
    private final EventPeriodRepository eventPeriodRepository;
    private final CategoryRepository categoryRepository;
    private final ItemClassificationRepository classificationRepository;
    private final ItemClassificationMapRepository itemClassificationMapRepository;
    private final ItemRepository itemRepository;
    private final MediaAssetRepository mediaAssetRepository;

    public ItemBulkUploadService(
            ExhibitionRepository exhibitionRepository,
            EventPeriodRepository eventPeriodRepository,
            CategoryRepository categoryRepository,
            ItemClassificationRepository classificationRepository,
            ItemClassificationMapRepository itemClassificationMapRepository,
            ItemRepository itemRepository,
            MediaAssetRepository mediaAssetRepository
    ) {
        this.exhibitionRepository = exhibitionRepository;
        this.eventPeriodRepository = eventPeriodRepository;
        this.categoryRepository = categoryRepository;
        this.classificationRepository = classificationRepository;
        this.itemClassificationMapRepository = itemClassificationMapRepository;
        this.itemRepository = itemRepository;
        this.mediaAssetRepository = mediaAssetRepository;
    }

    @Transactional
    public ItemBulkUploadResponse upload(MultipartFile file, AuthenticatedUser currentUser) {
        validateAdmin(currentUser);
        validateFile(file);

        int createdItems = 0;
        int createdMediaAssets = 0;
        int createdClassificationMappings = 0;
        DataFormatter formatter = new DataFormatter();

        try (InputStream inputStream = file.getInputStream(); Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheet(TEMPLATE_SHEET);
            if (sheet == null) {
                sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            }
            if (sheet == null) {
                throw new IllegalArgumentException("Invalid excel file: no worksheet");
            }

            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (isDataRowEmpty(row, formatter)) {
                    continue;
                }

                int excelRowNum = rowIndex + 1;
                UUID exhibitionId = parseUuid(cellValue(row, 0, formatter), "exhibition_id", excelRowNum);
                UUID eventPeriodId = parseUuid(cellValue(row, 1, formatter), "event_period_id", excelRowNum);
                String categoryName = required(cellValue(row, 3, formatter), "category_name", excelRowNum);
                String title = required(cellValue(row, 7, formatter), "title", excelRowNum);

                ExhibitionEntity exhibition = exhibitionRepository.findById(exhibitionId)
                        .orElseThrow(() -> new NotFoundException("Row " + excelRowNum + ": exhibition not found: " + exhibitionId));

                EventPeriodEntity eventPeriod = eventPeriodRepository.findByIdAndExhibition_Id(eventPeriodId, exhibitionId)
                        .orElseThrow(() -> new NotFoundException(
                                "Row " + excelRowNum + ": event period not found for exhibition: " + eventPeriodId));

                CategoryEntity category = categoryRepository.findByExhibition_IdAndName(exhibitionId, categoryName)
                        .orElseThrow(() -> new NotFoundException(
                                "Row " + excelRowNum + ": category not found for exhibition: " + categoryName));

                String description = nullable(cellValue(row, 8, formatter));
                String participantNames = nullable(cellValue(row, 9, formatter));
                String advisorNames = nullable(cellValue(row, 10, formatter));

                ItemEntity item = new ItemEntity(null, exhibition, category, eventPeriod, title, description);
                item.updateParticipantAndAdvisor(participantNames, advisorNames);
                item = itemRepository.save(item);
                createdItems++;

                MediaAssetEntity thumbnailMedia = createMediaIfPresent(item, exhibition, cellValue(row, 11, formatter));
                MediaAssetEntity posterMedia = createMediaIfPresent(item, exhibition, cellValue(row, 12, formatter));
                MediaAssetEntity presentationMedia = createMediaIfPresent(item, exhibition, cellValue(row, 13, formatter));

                if (thumbnailMedia != null) {
                    thumbnailMedia = mediaAssetRepository.save(thumbnailMedia);
                    createdMediaAssets++;
                }
                if (posterMedia != null) {
                    posterMedia = mediaAssetRepository.save(posterMedia);
                    createdMediaAssets++;
                }
                if (presentationMedia != null) {
                    presentationMedia = mediaAssetRepository.save(presentationMedia);
                    createdMediaAssets++;
                }

                item.updateMedia(thumbnailMedia, posterMedia, presentationMedia);

                Set<String> classificationNames = new LinkedHashSet<>();
                addIfPresent(classificationNames, cellValue(row, 4, formatter));
                addIfPresent(classificationNames, cellValue(row, 5, formatter));
                addIfPresent(classificationNames, cellValue(row, 6, formatter));

                if (!classificationNames.isEmpty()) {
                    List<ItemClassificationMapEntity> mappings = new ArrayList<>();
                    for (String classificationName : classificationNames) {
                        ItemClassificationEntity classification = classificationRepository.findByExhibition_IdAndName(
                                        exhibitionId, classificationName)
                                .orElseThrow(() -> new NotFoundException(
                                        "Row " + excelRowNum + ": classification not found for exhibition: " + classificationName));
                        mappings.add(new ItemClassificationMapEntity(null, item, classification));
                    }
                    itemClassificationMapRepository.saveAll(mappings);
                    createdClassificationMappings += mappings.size();
                }
            }
        } catch (IOException ex) {
            throw new IllegalArgumentException("Failed to read excel file", ex);
        }

        return new ItemBulkUploadResponse(createdItems, createdMediaAssets, createdClassificationMappings);
    }

    private MediaAssetEntity createMediaIfPresent(ItemEntity item, ExhibitionEntity exhibition, String objectKeyRaw) {
        String objectKey = nullable(objectKeyRaw);
        if (objectKey == null) {
            return null;
        }

        return new MediaAssetEntity(
                null,
                item,
                exhibition,
                objectKey,
                inferMediaType(objectKey),
                0L
        );
    }

    private String inferMediaType(String objectKey) {
        String lower = objectKey.toLowerCase();
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (lower.endsWith(".gif")) {
            return "image/gif";
        }
        if (lower.endsWith(".webp")) {
            return "image/webp";
        }
        if (lower.endsWith(".mp4")) {
            return "video/mp4";
        }
        if (lower.endsWith(".webm")) {
            return "video/webm";
        }
        if (lower.endsWith(".mov")) {
            return "video/quicktime";
        }
        return "application/octet-stream";
    }

    private void addIfPresent(Set<String> target, String value) {
        String normalized = nullable(value);
        if (normalized != null) {
            target.add(normalized);
        }
    }

    private boolean isDataRowEmpty(Row row, DataFormatter formatter) {
        if (row == null) {
            return true;
        }

        for (int col = 3; col <= 13; col++) {
            if (!cellValue(row, col, formatter).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private String required(String value, String columnName, int excelRowNum) {
        String normalized = nullable(value);
        if (normalized == null) {
            throw new IllegalArgumentException("Row " + excelRowNum + ": " + columnName + " is required");
        }
        return normalized;
    }

    private UUID parseUuid(String value, String columnName, int excelRowNum) {
        String normalized = required(value, columnName, excelRowNum);
        try {
            return UUID.fromString(normalized);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Row " + excelRowNum + ": invalid UUID in " + columnName + ": " + normalized);
        }
    }

    private String nullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String cellValue(Row row, int columnIndex, DataFormatter formatter) {
        if (row == null || row.getCell(columnIndex) == null) {
            return "";
        }
        return formatter.formatCellValue(row.getCell(columnIndex));
    }

    private void validateAdmin(AuthenticatedUser currentUser) {
        if (currentUser.role() != UserType.ADMIN) {
            throw new ForbiddenException("Admin role is required");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Excel file is required");
        }
    }
}
