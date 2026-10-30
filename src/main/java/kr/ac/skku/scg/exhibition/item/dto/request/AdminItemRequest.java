package kr.ac.skku.scg.exhibition.item.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import java.util.List;

public record AdminItemRequest(
        @NotNull UUID categoryId,
        UUID eventPeriodId,
        @NotBlank @Size(max = 200) String title,
        String description,
        String participantNames,
        String participantEmails,
        String advisorNames,
        boolean awarded,
        List<@NotNull UUID> classificationIds,
        Boolean removeThumbnail,
        Boolean removePoster,
        Boolean removePresentationVideo
) {
    /** 생략된 제거 플래그는 기존 미디어 유지로 해석한다. */
    public AdminItemRequest {
        removeThumbnail = Boolean.TRUE.equals(removeThumbnail);
        removePoster = Boolean.TRUE.equals(removePoster);
        removePresentationVideo = Boolean.TRUE.equals(removePresentationVideo);
    }

    /** 기존 JSON 클라이언트와 서비스 호출의 기본값을 유지한다. */
    public AdminItemRequest(UUID categoryId, UUID eventPeriodId, String title, String description,
            String participantNames, String participantEmails, String advisorNames, boolean awarded) {
        this(categoryId, eventPeriodId, title, description, participantNames, participantEmails, advisorNames,
                awarded, null, false, false, false);
    }
}
