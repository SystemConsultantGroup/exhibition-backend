package kr.ac.skku.scg.exhibition.item.controller;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import kr.ac.skku.scg.exhibition.exhibition.domain.ExhibitionEntity;
import kr.ac.skku.scg.exhibition.global.auth.resolver.AuthenticatedUser;
import kr.ac.skku.scg.exhibition.global.auth.resolver.CurrentUser;
import kr.ac.skku.scg.exhibition.global.tenant.CurrentExhibition;
import kr.ac.skku.scg.exhibition.item.dto.request.AdminItemRequest;
import kr.ac.skku.scg.exhibition.item.dto.response.ItemResponse;
import kr.ac.skku.scg.exhibition.item.service.AdminItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/admin/items")
@RequiredArgsConstructor
public class AdminItemController {
    private final AdminItemService service;

    /** 미디어 파일과 함께 아이템을 생성한다. */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ItemResponse> create(
            @Valid @RequestPart("request") AdminItemRequest request,
            @RequestPart(value = "thumbnailFile", required = false) MultipartFile thumbnailFile,
            @RequestPart(value = "posterFile", required = false) MultipartFile posterFile,
            @RequestPart(value = "presentationVideoFile", required = false) MultipartFile presentationVideoFile,
            @CurrentExhibition ExhibitionEntity exhibition,
            @CurrentUser AuthenticatedUser user
    ) {
        ItemResponse response = service.create(
                exhibition, request, thumbnailFile, posterFile, presentationVideoFile, user);
        return ResponseEntity.created(URI.create("/admin/items/" + response.id())).body(response);
    }

    /** 아이템 메타데이터를 수정한다. */
    @PutMapping("/{id}")
    public ResponseEntity<ItemResponse> update(@PathVariable UUID id, @Valid @RequestBody AdminItemRequest request,
            @CurrentExhibition ExhibitionEntity exhibition, @CurrentUser AuthenticatedUser user) {
        return ResponseEntity.ok(service.update(id, exhibition.getId(), request, user));
    }

    /** 아이템을 삭제한다. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id, @CurrentExhibition ExhibitionEntity exhibition,
            @CurrentUser AuthenticatedUser user) {
        service.delete(id, exhibition.getId(), user);
        return ResponseEntity.noContent().build();
    }
}
