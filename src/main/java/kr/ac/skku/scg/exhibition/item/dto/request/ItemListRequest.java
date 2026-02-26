package kr.ac.skku.scg.exhibition.item.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class ItemListRequest {

    @NotNull
    private UUID exhibitionId;

    private UUID categoryId;

    private UUID eventPeriodId;

    private UUID classificationId;

    private String q;

    public UUID getExhibitionId() {
        return exhibitionId;
    }

    public void setExhibitionId(UUID exhibitionId) {
        this.exhibitionId = exhibitionId;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(UUID categoryId) {
        this.categoryId = categoryId;
    }

    public UUID getEventPeriodId() {
        return eventPeriodId;
    }

    public void setEventPeriodId(UUID eventPeriodId) {
        this.eventPeriodId = eventPeriodId;
    }

    public UUID getClassificationId() {
        return classificationId;
    }

    public void setClassificationId(UUID classificationId) {
        this.classificationId = classificationId;
    }

    public String getQ() {
        return q;
    }

    public void setQ(String q) {
        this.q = q;
    }

}
