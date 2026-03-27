package kr.ac.skku.scg.exhibition.item.controller;

import kr.ac.skku.scg.exhibition.global.auth.resolver.AuthenticatedUser;
import kr.ac.skku.scg.exhibition.global.auth.resolver.CurrentUser;
import kr.ac.skku.scg.exhibition.item.dto.response.ItemBulkUploadResponse;
import kr.ac.skku.scg.exhibition.item.service.ItemBulkUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/admin/items/bulk")
@RequiredArgsConstructor
public class ItemBulkUploadController {

    private final ItemBulkUploadService itemBulkUploadService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ItemBulkUploadResponse> upload(
            @RequestPart("file") MultipartFile file,
            @CurrentUser AuthenticatedUser currentUser
    ) {
        ItemBulkUploadResponse response = itemBulkUploadService.upload(file, currentUser);
        return ResponseEntity.ok(response);
    }
}
