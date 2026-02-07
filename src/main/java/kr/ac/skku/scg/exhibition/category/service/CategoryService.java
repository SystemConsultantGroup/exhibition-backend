package kr.ac.skku.scg.exhibition.category.service;

import java.util.List;
import java.util.UUID;
import kr.ac.skku.scg.exhibition.category.domain.CategoryEntity;
import kr.ac.skku.scg.exhibition.category.dto.request.CategoryListRequest;
import kr.ac.skku.scg.exhibition.category.dto.response.CategoryResponse;
import kr.ac.skku.scg.exhibition.category.repository.CategoryRepository;
import kr.ac.skku.scg.exhibition.global.error.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public CategoryResponse get(UUID id) {
        CategoryEntity category = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Category not found: " + id));
        return toResponse(category);
    }

    public List<CategoryResponse> list(CategoryListRequest request) {
        return categoryRepository.findAllByExhibition_Id(request.getExhibitionId()).stream()
                .map(this::toResponse)
                .toList();
    }

    private CategoryResponse toResponse(CategoryEntity category) {
        return new CategoryResponse(
                category.getId(),
                category.getExhibition().getId(),
                category.getName());
    }
}
