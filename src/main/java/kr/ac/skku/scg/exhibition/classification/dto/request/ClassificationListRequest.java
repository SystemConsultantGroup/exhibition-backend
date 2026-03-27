package kr.ac.skku.scg.exhibition.classification.dto.request;

import java.util.UUID;

public class ClassificationListRequest {

    private UUID exhibitionId;

    public UUID getExhibitionId() {
        return exhibitionId;
    }

    public void setExhibitionId(UUID exhibitionId) {
        this.exhibitionId = exhibitionId;
    }
}
