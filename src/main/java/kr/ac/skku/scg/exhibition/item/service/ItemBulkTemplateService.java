package kr.ac.skku.scg.exhibition.item.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import kr.ac.skku.scg.exhibition.category.repository.CategoryRepository;
import kr.ac.skku.scg.exhibition.classification.repository.ItemClassificationRepository;
import kr.ac.skku.scg.exhibition.eventperiod.domain.EventPeriodEntity;
import kr.ac.skku.scg.exhibition.eventperiod.repository.EventPeriodRepository;
import kr.ac.skku.scg.exhibition.exhibition.repository.ExhibitionRepository;
import kr.ac.skku.scg.exhibition.global.auth.resolver.AuthenticatedUser;
import kr.ac.skku.scg.exhibition.global.error.ForbiddenException;
import kr.ac.skku.scg.exhibition.global.error.NotFoundException;
import kr.ac.skku.scg.exhibition.item.dto.request.ItemBulkTemplateRequest;
import kr.ac.skku.scg.exhibition.item.dto.response.ItemBulkTemplateFile;
import kr.ac.skku.scg.exhibition.user.domain.UserType;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Name;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ItemBulkTemplateService {

    private static final String TEMPLATE_SHEET = "items_template";
    private static final String OPTIONS_SHEET = "options";
    private static final int HEADER_ROW_INDEX = 0;
    private static final int DATA_ROW_START = 1;
    private static final int DATA_ROW_END = 1000;
    private static final int OPTIONS_START_ROW = 4;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    private final ExhibitionRepository exhibitionRepository;
    private final EventPeriodRepository eventPeriodRepository;
    private final CategoryRepository categoryRepository;
    private final ItemClassificationRepository classificationRepository;

    public ItemBulkTemplateService(
            ExhibitionRepository exhibitionRepository,
            EventPeriodRepository eventPeriodRepository,
            CategoryRepository categoryRepository,
            ItemClassificationRepository classificationRepository
    ) {
        this.exhibitionRepository = exhibitionRepository;
        this.eventPeriodRepository = eventPeriodRepository;
        this.categoryRepository = categoryRepository;
        this.classificationRepository = classificationRepository;
    }

    public ItemBulkTemplateFile generateTemplate(ItemBulkTemplateRequest request, AuthenticatedUser currentUser) {
        validateAdmin(currentUser);

        UUID exhibitionId = request.getExhibitionId();
        UUID eventPeriodId = request.getEventPeriodId();

        exhibitionRepository.findById(exhibitionId)
                .orElseThrow(() -> new NotFoundException("Exhibition not found: " + exhibitionId));

        EventPeriodEntity eventPeriod = eventPeriodRepository.findByIdAndExhibition_Id(eventPeriodId, exhibitionId)
                .orElseThrow(() -> new NotFoundException(
                        "Event period not found for exhibition: exhibitionId=" + exhibitionId + ", eventPeriodId=" + eventPeriodId));

        List<String> categories = categoryRepository.findAllByExhibition_Id(exhibitionId).stream()
                .map(category -> category.getName().trim())
                .filter(name -> !name.isBlank())
                .distinct()
                .sorted(Comparator.naturalOrder())
                .toList();

        List<String> classifications = classificationRepository.findAllByExhibition_Id(exhibitionId).stream()
                .map(classification -> classification.getName().trim())
                .filter(name -> !name.isBlank())
                .distinct()
                .sorted(Comparator.naturalOrder())
                .toList();

        byte[] bytes = createWorkbookBytes(exhibitionId, eventPeriod, categories, classifications);
        return new ItemBulkTemplateFile(buildFileName(exhibitionId, eventPeriodId), bytes);
    }

    private byte[] createWorkbookBytes(
            UUID exhibitionId,
            EventPeriodEntity eventPeriod,
            List<String> categories,
            List<String> classifications
    ) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet templateSheet = workbook.createSheet(TEMPLATE_SHEET);
            Sheet optionsSheet = workbook.createSheet(OPTIONS_SHEET);

            CellStyle headerStyle = createHeaderStyle(workbook);
            createTemplateHeader(templateSheet, headerStyle);
            fillFixedColumns(templateSheet, exhibitionId, eventPeriod.getId(), eventPeriod.getName());
            populateOptionsSheet(optionsSheet, exhibitionId, eventPeriod.getId(), eventPeriod.getName(), categories, classifications);
            registerNamedRange(workbook, "category_options", OPTIONS_SHEET + "!$A$" + (OPTIONS_START_ROW + 1) + ":$A$"
                    + Math.max(OPTIONS_START_ROW + categories.size(), OPTIONS_START_ROW + 1));
            registerNamedRange(workbook, "classification_options", OPTIONS_SHEET + "!$B$" + (OPTIONS_START_ROW + 1) + ":$B$"
                    + Math.max(OPTIONS_START_ROW + classifications.size(), OPTIONS_START_ROW + 1));
            applyDropDown(templateSheet, 3, "category_options");
            applyDropDown(templateSheet, 4, "classification_options");
            applyDropDown(templateSheet, 5, "classification_options");
            applyDropDown(templateSheet, 6, "classification_options");

            workbook.setSheetHidden(workbook.getSheetIndex(optionsSheet), true);

            for (int i = 0; i <= 13; i++) {
                templateSheet.autoSizeColumn(i);
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to create item bulk template", ex);
        }
    }

    private void createTemplateHeader(Sheet templateSheet, CellStyle headerStyle) {
        String[] headers = {
                "exhibition_id",
                "event_period_id",
                "event_period_name",
                "category_name",
                "classification_1_name",
                "classification_2_name",
                "classification_3_name",
                "title",
                "description",
                "participant_names_csv",
                "advisor_names_csv",
                "thumbnail_object_key",
                "poster_object_key",
                "presentation_video_object_key"
        };

        Row headerRow = templateSheet.createRow(HEADER_ROW_INDEX);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    private void fillFixedColumns(Sheet templateSheet, UUID exhibitionId, UUID eventPeriodId, String eventPeriodName) {
        for (int rowIndex = DATA_ROW_START; rowIndex <= DATA_ROW_END; rowIndex++) {
            Row row = templateSheet.createRow(rowIndex);
            row.createCell(0).setCellValue(exhibitionId.toString());
            row.createCell(1).setCellValue(eventPeriodId.toString());
            row.createCell(2).setCellValue(eventPeriodName);
        }
    }

    private void populateOptionsSheet(
            Sheet optionsSheet,
            UUID exhibitionId,
            UUID eventPeriodId,
            String eventPeriodName,
            List<String> categories,
            List<String> classifications
    ) {
        Row metadataHeader = optionsSheet.createRow(0);
        metadataHeader.createCell(0).setCellValue("exhibition_id");
        metadataHeader.createCell(1).setCellValue("event_period_id");
        metadataHeader.createCell(2).setCellValue("event_period_name");

        Row metadataValue = optionsSheet.createRow(1);
        metadataValue.createCell(0).setCellValue(exhibitionId.toString());
        metadataValue.createCell(1).setCellValue(eventPeriodId.toString());
        metadataValue.createCell(2).setCellValue(eventPeriodName);

        Row optionsHeader = optionsSheet.createRow(OPTIONS_START_ROW - 1);
        optionsHeader.createCell(0).setCellValue("category_options");
        optionsHeader.createCell(1).setCellValue("classification_options");

        int max = Math.max(categories.size(), classifications.size());
        for (int i = 0; i < max; i++) {
            Row row = optionsSheet.createRow(OPTIONS_START_ROW + i);
            if (i < categories.size()) {
                row.createCell(0).setCellValue(categories.get(i));
            }
            if (i < classifications.size()) {
                row.createCell(1).setCellValue(classifications.get(i));
            }
        }
    }

    private void registerNamedRange(Workbook workbook, String name, String formulaRef) {
        Name namedRange = workbook.createName();
        namedRange.setNameName(name);
        namedRange.setRefersToFormula(formulaRef);
    }

    private void applyDropDown(Sheet templateSheet, int columnIndex, String optionsNamedRange) {
        DataValidationHelper helper = templateSheet.getDataValidationHelper();
        DataValidationConstraint constraint = helper.createFormulaListConstraint(optionsNamedRange);
        CellRangeAddressList addressList = new CellRangeAddressList(DATA_ROW_START, DATA_ROW_END, columnIndex, columnIndex);
        DataValidation validation = helper.createValidation(constraint, addressList);
        validation.setSuppressDropDownArrow(true);
        validation.setShowErrorBox(true);
        validation.createErrorBox("Invalid value", "Select a value from the dropdown.");
        templateSheet.addValidationData(validation);
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private String buildFileName(UUID exhibitionId, UUID eventPeriodId) {
        String date = LocalDate.now().format(DATE_FORMATTER);
        return "item-bulk-template-" + exhibitionId + "-" + eventPeriodId + "-" + date + ".xlsx";
    }

    private void validateAdmin(AuthenticatedUser currentUser) {
        if (currentUser.role() != UserType.ADMIN) {
            throw new ForbiddenException("Admin role is required");
        }
    }
}
