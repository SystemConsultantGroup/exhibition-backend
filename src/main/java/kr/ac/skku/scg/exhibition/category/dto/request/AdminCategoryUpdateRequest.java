package kr.ac.skku.scg.exhibition.category.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminCategoryUpdateRequest(@NotBlank @Size(max = 128) String name) {
}
