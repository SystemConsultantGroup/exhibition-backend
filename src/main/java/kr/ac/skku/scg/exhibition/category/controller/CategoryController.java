package kr.ac.skku.scg.exhibition.category.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import kr.ac.skku.scg.exhibition.exhibition.domain.ExhibitionEntity;
import kr.ac.skku.scg.exhibition.global.tenant.CurrentExhibition;
import kr.ac.skku.scg.exhibition.category.dto.request.CategoryListRequest;
import kr.ac.skku.scg.exhibition.category.dto.response.CategoryResponse;
import kr.ac.skku.scg.exhibition.category.service.CategoryService;
import kr.ac.skku.scg.exhibition.global.dto.ListResponse;
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
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> get(
            @PathVariable UUID id,
            @CurrentExhibition ExhibitionEntity currentExhibition) {
        return ResponseEntity.ok(categoryService.get(id, currentExhibition.getId()));
    }

    @GetMapping
    public ResponseEntity<ListResponse<CategoryResponse>> list(
            @Valid @ModelAttribute CategoryListRequest request,
            @CurrentExhibition ExhibitionEntity currentExhibition) {
        request.setExhibitionId(currentExhibition.getId());
        List<CategoryResponse> items = categoryService.list(request);
        return ResponseEntity.ok(ListResponse.of(items));
    }
}
