package kr.ac.skku.scg.exhibition.item.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class ItemBulkTemplateRequest {

    @NotNull
    private UUID exhibitionId;

    @NotNull
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
