package kr.ac.skku.scg.exhibition.item.service;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
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
import kr.ac.skku.scg.exhibition.global.config.MinioProperties;
import kr.ac.skku.scg.exhibition.global.error.NotFoundException;
import kr.ac.skku.scg.exhibition.item.domain.ItemEntity;
import kr.ac.skku.scg.exhibition.item.dto.response.ItemBulkUploadResponse;
import kr.ac.skku.scg.exhibition.item.repository.ItemRepository;
import kr.ac.skku.scg.exhibition.media.domain.MediaAssetEntity;
import kr.ac.skku.scg.exhibition.media.repository.MediaAssetRepository;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
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
    private final MediaAssetRepository mediaAssets;
    private final MinioClient minio;
    private final MinioProperties minioProperties;
    private final AdminAuthorizationService authorization;

    public ItemBulkUploadService(ExhibitionRepository exhibitions, EventPeriodRepository periods,
            CategoryRepository categories, ItemClassificationRepository classifications,
            ItemClassificationMapRepository mappings, ItemRepository items, MediaAssetRepository mediaAssets,
            MinioClient minio, MinioProperties minioProperties, AdminAuthorizationService authorization) {
        this.exhibitions = exhibitions;
        this.periods = periods;
        this.categories = categories;
        this.classifications = classifications;
        this.mappings = mappings;
        this.items = items;
        this.mediaAssets = mediaAssets;
        this.minio = minio;
        this.minioProperties = minioProperties;
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
        List<String> uploadedKeys = new ArrayList<>();
        int itemCount = 0, mediaCount = 0, mappingCount = 0;
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
                CategoryEntity category = categories.findByExhibition_IdAndName(currentExhibitionId, categoryName)
                        .orElseThrow(() -> new NotFoundException("Row " + rowNumber + ": category not found: " + categoryName));
                ItemEntity item = new ItemEntity(null, exhibition, category, period, title, nullable(cellValue(row, 3, formatter)));
                item.updateMetadata(category, period, title, nullable(cellValue(row, 3, formatter)),
                        nullable(cellValue(row, 4, formatter)), nullable(cellValue(row, 5, formatter)),
                        nullable(cellValue(row, 6, formatter)), parseAwarded(cellValue(row, 7, formatter), rowNumber));
                item = items.save(item);
                itemCount++;

                MediaAssetEntity thumbnail = createMedia(item, exhibition, cellValue(row, 8, formatter), "thumbnail_file_name",
                        rowNumber, true, files, used, uploadedKeys);
                MediaAssetEntity poster = createMedia(item, exhibition, cellValue(row, 9, formatter), "poster_file_name",
                        rowNumber, true, files, used, uploadedKeys);
                MediaAssetEntity video = createMedia(item, exhibition, cellValue(row, 10, formatter), "presentation_video_file_name",
                        rowNumber, false, files, used, uploadedKeys);
                if (thumbnail != null) {
                    thumbnail = mediaAssets.save(thumbnail);
                    mediaCount++;
                }
                if (poster != null) {
                    poster = mediaAssets.save(poster);
                    mediaCount++;
                }
                if (video != null) {
                    video = mediaAssets.save(video);
                    mediaCount++;
                }
                item.updateMedia(thumbnail, poster, video);

                String classificationName = nullable(cellValue(row, 1, formatter));
                if (classificationName != null) {
                    ItemClassificationEntity classification = classifications.findByExhibition_IdAndName(
                            currentExhibitionId, classificationName).orElseThrow(() -> new NotFoundException(
                                    "Row " + rowNumber + ": classification not found: " + classificationName));
                    mappings.save(new ItemClassificationMapEntity(null, item, classification));
                    mappingCount++;
                }
            }
            Set<String> unused = new HashSet<>(files.keySet());
            unused.removeAll(used);
            if (!unused.isEmpty()) {
                throw new IllegalArgumentException("Unused mediaFiles: " + unused);
            }
            cleanupOnRollback(uploadedKeys);
        } catch (IOException ex) {
            cleanup(uploadedKeys);
            throw new IllegalArgumentException("Failed to read excel file", ex);
        } catch (RuntimeException ex) {
            cleanup(uploadedKeys);
            throw ex;
        }
        return new ItemBulkUploadResponse(itemCount, mediaCount, mappingCount);
    }

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

    /** 미디어 참조를 검증하고 MinIO 업로드 엔티티를 만든다. */
    private MediaAssetEntity createMedia(ItemEntity item, ExhibitionEntity exhibition, String raw, String column,
            int row, boolean image, Map<String, MultipartFile> files, Set<String> used, List<String> uploadedKeys) {
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
        String type = mediaType(name);
        if ((image && !type.startsWith("image/")) || (!image && !type.startsWith("video/"))) {
            throw new IllegalArgumentException("Row " + row + ": invalid media type for " + column + ": " + name);
        }
        validateSignature(file, type, name);
        used.add(name);
        String key = "exhibitions/" + exhibition.getId() + "/items/" + UUID.randomUUID() + "-" + name;
        upload(file, key, type, uploadedKeys);
        return new MediaAssetEntity(null, item, exhibition, key, type, file.getSize());
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

    /** 파일 확장자와 실제 파일 시그니처가 일치하는지 확인한다. */
    private void validateSignature(MultipartFile file, String type, String name) {
        byte[] prefix;
        try (InputStream input = file.getInputStream()) {
            prefix = input.readNBytes(12);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Failed to inspect media file: " + name, ex);
        }

        boolean valid;
        if ("image/png".equals(type)) {
            valid = startsWith(prefix, 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a);
        } else if ("image/jpeg".equals(type)) {
            valid = startsWith(prefix, 0xff, 0xd8, 0xff);
        } else if ("image/gif".equals(type)) {
            valid = startsWith(prefix, 'G', 'I', 'F', '8', '7', 'a')
                    || startsWith(prefix, 'G', 'I', 'F', '8', '9', 'a');
        } else if ("image/webp".equals(type)) {
            valid = startsWith(prefix, 'R', 'I', 'F', 'F')
                    && matchesAt(prefix, 8, 'W', 'E', 'B', 'P');
        } else if ("video/mp4".equals(type) || "video/quicktime".equals(type)) {
            valid = matchesAt(prefix, 4, 'f', 't', 'y', 'p');
        } else if ("video/webm".equals(type)) {
            valid = startsWith(prefix, 0x1a, 0x45, 0xdf, 0xa3);
        } else {
            valid = false;
        }
        if (!valid) {
            throw new IllegalArgumentException("Media file signature does not match extension: " + name);
        }
    }

    /** 바이트 배열이 지정 위치의 시그니처로 시작하는지 확인한다. */
    private boolean startsWith(byte[] bytes, int... signature) {
        return matchesAt(bytes, 0, signature);
    }

    /** 바이트 배열의 지정 위치가 시그니처와 일치하는지 확인한다. */
    private boolean matchesAt(byte[] bytes, int offset, int... signature) {
        if (bytes.length < offset + signature.length) return false;
        for (int i = 0; i < signature.length; i++) {
            if ((bytes[offset + i] & 0xff) != signature[i]) return false;
        }
        return true;
    }

    /** MinIO에 파일을 저장한다. */
    private void upload(MultipartFile file, String key, String type, List<String> uploadedKeys) {
        uploadedKeys.add(key);
        try (InputStream input = file.getInputStream()) {
            minio.putObject(PutObjectArgs.builder().bucket(minioProperties.getBucket()).object(key)
                    .stream(input, file.getSize(), -1).contentType(type).build());
        } catch (Exception ex) { throw new IllegalStateException("Failed to upload media file: " + file.getOriginalFilename(), ex); }
    }

    /** 트랜잭션 롤백 시 MinIO 객체를 정리한다. */
    private void cleanupOnRollback(List<String> keys) {
        if (!keys.isEmpty() && TransactionSynchronizationManager.isSynchronizationActive())
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCompletion(int status) { if (status != STATUS_COMMITTED) cleanup(keys); }
            });
    }

    /** 업로드된 MinIO 객체를 최선 노력으로 정리한다. */
    private void cleanup(List<String> keys) {
        for (String key : keys) try {
            minio.removeObject(RemoveObjectArgs.builder().bucket(minioProperties.getBucket()).object(key).build());
        } catch (Exception ignored) { }
    }

    /** 파일 확장자를 MIME 타입으로 변환한다. */
    private String mediaType(String name) {
        String lower = name.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".mp4")) return "video/mp4";
        if (lower.endsWith(".webm")) return "video/webm";
        if (lower.endsWith(".mov")) return "video/quicktime";
        return "application/octet-stream";
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
