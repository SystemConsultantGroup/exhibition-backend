package kr.ac.skku.scg.exhibition.category.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import kr.ac.skku.scg.exhibition.category.dto.request.AdminCategoryOrderRequest;
import kr.ac.skku.scg.exhibition.category.dto.request.AdminCategoryUpdateRequest;
import kr.ac.skku.scg.exhibition.category.dto.response.CategoryResponse;
import kr.ac.skku.scg.exhibition.category.service.CategoryService;
import kr.ac.skku.scg.exhibition.exhibition.domain.ExhibitionEntity;
import kr.ac.skku.scg.exhibition.global.auth.resolver.AuthenticatedUser;
import kr.ac.skku.scg.exhibition.global.auth.resolver.CurrentUser;
import kr.ac.skku.scg.exhibition.global.dto.ListResponse;
import kr.ac.skku.scg.exhibition.global.tenant.CurrentExhibition;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {
    private final CategoryService categoryService;

    /** 카테고리 이름을 수정한다. */
    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> update(@PathVariable UUID id,
            @Valid @RequestBody AdminCategoryUpdateRequest request,
            @CurrentExhibition ExhibitionEntity exhibition, @CurrentUser AuthenticatedUser user) {
        return ResponseEntity.ok(categoryService.update(id, exhibition.getId(), request, user));
    }

    /** 카테고리 전체 순서를 수정한다. */
    @PutMapping("/order")
    public ResponseEntity<ListResponse<CategoryResponse>> reorder(@Valid @RequestBody AdminCategoryOrderRequest request,
            @CurrentExhibition ExhibitionEntity exhibition, @CurrentUser AuthenticatedUser user) {
        List<CategoryResponse> items = categoryService.reorder(exhibition.getId(), request, user);
        return ResponseEntity.ok(ListResponse.of(items));
    }
}
