package kr.ac.skku.scg.exhibition.board.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminBoardRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 16000) String content
) {
}
