package kr.ac.skku.scg.exhibition.category.controller;

import static kr.ac.skku.scg.exhibition.category.dto.CategoryDtos.*;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import kr.ac.skku.scg.exhibition.category.service.CategoryService;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping("/exhibitions/{exhibitionId}/categories/tree")
    public List<CategoryTreeNode> getTree(@PathVariable UUID exhibitionId) {
        return categoryService.buildTreeResponse(exhibitionId);
    }
}
