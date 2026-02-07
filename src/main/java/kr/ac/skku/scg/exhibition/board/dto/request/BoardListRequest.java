package kr.ac.skku.scg.exhibition.board.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class BoardListRequest {

    @NotNull
    private UUID exhibitionId;

    public UUID getExhibitionId() {
        return exhibitionId;
    }

    public void setExhibitionId(UUID exhibitionId) {
        this.exhibitionId = exhibitionId;
    }
}
