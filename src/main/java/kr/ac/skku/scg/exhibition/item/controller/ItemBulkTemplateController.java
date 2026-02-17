package kr.ac.skku.scg.exhibition.item.controller;

import jakarta.validation.Valid;
import kr.ac.skku.scg.exhibition.global.auth.resolver.AuthenticatedUser;
import kr.ac.skku.scg.exhibition.global.auth.resolver.CurrentUser;
import kr.ac.skku.scg.exhibition.item.dto.request.ItemBulkTemplateRequest;
import kr.ac.skku.scg.exhibition.item.dto.response.ItemBulkTemplateFile;
import kr.ac.skku.scg.exhibition.item.service.ItemBulkTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/admin/items/bulk")
@RequiredArgsConstructor
public class ItemBulkTemplateController {

    private static final String EXCEL_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final ItemBulkTemplateService itemBulkTemplateService;

    @GetMapping("/template")
    public ResponseEntity<ByteArrayResource> downloadTemplate(
            @Valid @ModelAttribute ItemBulkTemplateRequest request,
            @CurrentUser AuthenticatedUser currentUser
    ) {
        ItemBulkTemplateFile file = itemBulkTemplateService.generateTemplate(request, currentUser);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(EXCEL_CONTENT_TYPE))
                .contentLength(file.bytes().length)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(file.fileName()).build().toString())
                .body(new ByteArrayResource(file.bytes()));
    }
}
