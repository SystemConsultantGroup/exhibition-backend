package kr.ac.skku.scg.exhibition.category.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class CategoryDtos {

    private CategoryDtos() {
    }

    public record CategoryTreeNode(
        UUID id,
        String name,
        int orderIndex,
        String path,
        int depth,
        List<CategoryTreeNode> children
    ) {
    }

    public record CategoryResponse(
        UUID id,
        UUID exhibitionId,
        UUID parentId,
        String name,
        int orderIndex,
        String path,
        int depth
    ) {
    }

    public record CreateCategoryRequest(
        @NotBlank @Size(max = 128) String name,
        UUID parentId,
        @Min(0) Integer orderIndex
    ) {
    }

    public record PatchCategoryRequest(
        @Size(max = 128) String name,
        UUID parentId,
        @Min(0) Integer orderIndex
    ) {
    }
}
