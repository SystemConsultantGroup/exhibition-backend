package kr.ac.skku.scg.exhibition.category.dto.request;

import java.util.UUID;

public class CategoryListRequest {

    private UUID exhibitionId;

    public UUID getExhibitionId() {
        return exhibitionId;
    }

    public void setExhibitionId(UUID exhibitionId) {
        this.exhibitionId = exhibitionId;
    }
}
