package kr.ac.skku.scg.exhibition.item.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class ItemListRequest {

    @NotNull
    private UUID exhibitionId;

    private UUID categoryId;

    private UUID eventPeriodId;

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
}
