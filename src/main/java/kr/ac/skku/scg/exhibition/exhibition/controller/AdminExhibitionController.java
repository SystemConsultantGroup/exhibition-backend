package kr.ac.skku.scg.exhibition.exhibition.controller;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import kr.ac.skku.scg.exhibition.exhibition.dto.request.AdminExhibitionCreateRequest;
import kr.ac.skku.scg.exhibition.exhibition.dto.request.AdminExhibitionUpdateRequest;
import kr.ac.skku.scg.exhibition.exhibition.dto.response.ExhibitionResponse;
import kr.ac.skku.scg.exhibition.exhibition.service.AdminExhibitionService;
import kr.ac.skku.scg.exhibition.global.auth.resolver.AuthenticatedUser;
import kr.ac.skku.scg.exhibition.global.auth.resolver.CurrentUser;
import kr.ac.skku.scg.exhibition.global.dto.ListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/admin/exhibitions")
@RequiredArgsConstructor
public class AdminExhibitionController {

    private final AdminExhibitionService adminExhibitionService;

    /** 현재 관리자가 접근할 수 있는 전시회 목록을 조회한다. */
    @GetMapping
    public ResponseEntity<ListResponse<ExhibitionResponse>> list(@CurrentUser AuthenticatedUser currentUser) {
        return ResponseEntity.ok(ListResponse.of(adminExhibitionService.listAccessible(currentUser)));
    }

    /** 슈퍼유저가 미디어 파일과 함께 전시회를 생성한다. */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ExhibitionResponse> create(
            @Valid @RequestPart("request") AdminExhibitionCreateRequest request,
            @RequestPart(value = "logoFile", required = false) MultipartFile logoFile,
            @RequestPart(value = "bannerFile", required = false) MultipartFile bannerFile,
            @RequestPart(value = "popupImageFile", required = false) MultipartFile popupImageFile,
            @RequestPart(value = "introVideoFile", required = false) MultipartFile introVideoFile,
            @CurrentUser AuthenticatedUser currentUser
    ) {
        ExhibitionResponse response = adminExhibitionService.create(
                request, logoFile, bannerFile, popupImageFile, introVideoFile, currentUser);
        return ResponseEntity.created(URI.create("/admin/exhibitions/" + response.id())).body(response);
    }

    /** 전시회 관리자가 전시회 기본 정보를 수정한다. */
    @PutMapping("/{exhibitionId}")
    public ResponseEntity<ExhibitionResponse> update(
            @PathVariable UUID exhibitionId,
            @Valid @RequestBody AdminExhibitionUpdateRequest request,
            @CurrentUser AuthenticatedUser currentUser
    ) {
        return ResponseEntity.ok(adminExhibitionService.update(exhibitionId, request, currentUser));
    }

    /** 슈퍼유저가 전시회를 삭제한다. */
    @DeleteMapping("/{exhibitionId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID exhibitionId,
            @CurrentUser AuthenticatedUser currentUser
    ) {
        adminExhibitionService.delete(exhibitionId, currentUser);
        return ResponseEntity.noContent().build();
    }
}
