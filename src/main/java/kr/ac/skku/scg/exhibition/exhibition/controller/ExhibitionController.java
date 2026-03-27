package kr.ac.skku.scg.exhibition.exhibition.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import kr.ac.skku.scg.exhibition.exhibition.domain.ExhibitionEntity;
import kr.ac.skku.scg.exhibition.exhibition.dto.request.ExhibitionListRequest;
import kr.ac.skku.scg.exhibition.exhibition.dto.response.ExhibitionResponse;
import kr.ac.skku.scg.exhibition.exhibition.dto.response.ExhibitionSlugResponse;
import kr.ac.skku.scg.exhibition.exhibition.service.ExhibitionService;
import kr.ac.skku.scg.exhibition.global.dto.ListResponse;
import kr.ac.skku.scg.exhibition.global.tenant.CurrentExhibition;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/exhibitions")
@RequiredArgsConstructor
public class ExhibitionController {

    private final ExhibitionService exhibitionService;

    @GetMapping("/{id}")
    public ResponseEntity<ExhibitionResponse> get(
            @PathVariable UUID id,
            @CurrentExhibition ExhibitionEntity currentExhibition) {
        // Path UUID is kept only to preserve the existing API shape; actual lookup uses the current exhibition from headers.
        return ResponseEntity.ok(exhibitionService.get(currentExhibition));
    }

    @GetMapping
    public ResponseEntity<ListResponse<ExhibitionResponse>> list(
            @Valid @ModelAttribute ExhibitionListRequest request,
            @CurrentExhibition ExhibitionEntity currentExhibition) {
        List<ExhibitionResponse> items = exhibitionService.list(request, currentExhibition);
        return ResponseEntity.ok(ListResponse.of(items));
    }

    @GetMapping("/slug")
    public ResponseEntity<ExhibitionSlugResponse> getSlug(
            @CurrentExhibition ExhibitionEntity currentExhibition) {
        return ResponseEntity.ok(exhibitionService.getSlug(currentExhibition));
    }
}
