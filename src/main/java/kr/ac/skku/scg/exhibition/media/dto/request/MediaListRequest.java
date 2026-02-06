package kr.ac.skku.scg.exhibition.media.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class MediaListRequest {

    @NotNull
    private UUID exhibitionId;

    private UUID itemId;

    public UUID getExhibitionId() {
        return exhibitionId;
    }

    public void setExhibitionId(UUID exhibitionId) {
        this.exhibitionId = exhibitionId;
    }

    public UUID getItemId() {
        return itemId;
    }

    public void setItemId(UUID itemId) {
        this.itemId = itemId;
    }
}
