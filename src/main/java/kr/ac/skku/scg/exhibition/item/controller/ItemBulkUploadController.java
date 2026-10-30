package kr.ac.skku.scg.exhibition.item.controller;

import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestHeader;
import kr.ac.skku.scg.exhibition.exhibition.domain.ExhibitionEntity;
import kr.ac.skku.scg.exhibition.global.auth.resolver.AuthenticatedUser;
import kr.ac.skku.scg.exhibition.global.auth.resolver.CurrentUser;
import kr.ac.skku.scg.exhibition.global.tenant.CurrentExhibition;
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

@Slf4j
@Validated
@RestController
@RequestMapping("/admin/items/bulk")
@RequiredArgsConstructor
public class ItemBulkUploadController {

    private final ItemBulkUploadService itemBulkUploadService;

    /** 등록 결과와 요청 ID, 처리 시간을 기록한다 (multipart 수신 이후 단계). */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ItemBulkUploadResponse> upload(
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "mediaFiles", required = false) List<MultipartFile> mediaFiles,
            @CurrentExhibition ExhibitionEntity currentExhibition,
            @CurrentUser AuthenticatedUser currentUser,
            @RequestHeader(value = "X-Upload-Id", required = false) String requestedId
    ) {
        String uploadId = requestedId != null && requestedId.matches("[0-9a-fA-F-]{36}")
                ? requestedId : UUID.randomUUID().toString();
        long start = System.nanoTime();
        log.info("Bulk upload processing started: uploadId={}, exhibitionId={}, mediaFiles={}",
                uploadId, currentExhibition.getId(), mediaFiles == null ? 0 : mediaFiles.size());
        ItemBulkUploadResponse response = itemBulkUploadService.upload(
                file, mediaFiles, currentExhibition.getId(), currentUser);
        log.info("Bulk upload committed: uploadId={}, processingMs={}, items={}, media={}", uploadId,
                (System.nanoTime() - start) / 1_000_000, response.createdItems(), response.createdMediaAssets());
        return ResponseEntity.ok().header("X-Upload-Id", uploadId).body(response);
    }
}
