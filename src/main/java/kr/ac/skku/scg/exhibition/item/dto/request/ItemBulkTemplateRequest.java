package kr.ac.skku.scg.exhibition.item.dto.request;

import java.util.UUID;

public class ItemBulkTemplateRequest {

    private UUID exhibitionId;

    @jakarta.validation.constraints.NotNull
    private UUID eventPeriodId;

    public UUID getExhibitionId() {
        return exhibitionId;
    }

    public void setExhibitionId(UUID exhibitionId) {
        this.exhibitionId = exhibitionId;
    }

    public UUID getEventPeriodId() {
        return eventPeriodId;
    }

    public void setEventPeriodId(UUID eventPeriodId) {
        this.eventPeriodId = eventPeriodId;
    }
}
