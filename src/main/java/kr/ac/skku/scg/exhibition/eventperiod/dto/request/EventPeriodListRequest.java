package kr.ac.skku.scg.exhibition.eventperiod.dto.request;

import java.util.UUID;

public class EventPeriodListRequest {

    private UUID exhibitionId;

    public UUID getExhibitionId() {
        return exhibitionId;
    }

    public void setExhibitionId(UUID exhibitionId) {
        this.exhibitionId = exhibitionId;
    }
}
