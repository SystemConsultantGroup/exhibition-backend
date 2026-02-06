package kr.ac.skku.scg.exhibition.category.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import kr.ac.skku.scg.exhibition.category.dto.request.CategoryListRequest;
import kr.ac.skku.scg.exhibition.category.dto.response.CategoryResponse;
import kr.ac.skku.scg.exhibition.category.service.CategoryService;
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
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(categoryService.get(id));
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> list(@Valid @ModelAttribute CategoryListRequest request) {
        return ResponseEntity.ok(categoryService.list(request));
    }
}
