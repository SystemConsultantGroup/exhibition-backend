package kr.ac.skku.scg.exhibition.board.dto.request;

import java.util.UUID;

public class BoardListRequest {

    private UUID exhibitionId;

    public UUID getExhibitionId() {
        return exhibitionId;
    }

    public void setExhibitionId(UUID exhibitionId) {
        this.exhibitionId = exhibitionId;
    }
}
