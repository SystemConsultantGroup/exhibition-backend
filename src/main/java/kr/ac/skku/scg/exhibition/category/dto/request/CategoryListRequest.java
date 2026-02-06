package kr.ac.skku.scg.exhibition.category.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class CategoryListRequest {

    @NotNull
    private UUID exhibitionId;

    public UUID getExhibitionId() {
        return exhibitionId;
    }

    public void setExhibitionId(UUID exhibitionId) {
        this.exhibitionId = exhibitionId;
    }
}
