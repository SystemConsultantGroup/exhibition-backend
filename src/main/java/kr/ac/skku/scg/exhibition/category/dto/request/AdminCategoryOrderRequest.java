package kr.ac.skku.scg.exhibition.category.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record AdminCategoryOrderRequest(@NotEmpty List<@NotNull UUID> categoryIds) {
}
