package kr.ac.skku.scg.exhibition.media.controller;

import java.util.UUID;
import kr.ac.skku.scg.exhibition.media.dto.response.MediaFileResponse;
import kr.ac.skku.scg.exhibition.media.service.MediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/media")
@RequiredArgsConstructor
public class MediaController {

    private static final String CACHE_CONTROL_VALUE = "public, max-age=31536000, immutable";

    private final MediaService mediaService;

    /** 기존 HTTP 계약을 유지하면서 파일 본문을 스트리밍한다. */
    @GetMapping("/{id}")
    public ResponseEntity<Resource> get(@PathVariable UUID id) {
        MediaFileResponse file = mediaService.getFile(id);
        MediaType mediaType = resolveMediaType(file.contentType());

        return ResponseEntity.ok()
                .header("Cache-Control", CACHE_CONTROL_VALUE)
                .contentType(mediaType)
                .contentLength(file.contentLength())
                .body(file.resource());
    }

    /** 잘못되거나 누락된 MIME 타입은 바이너리 타입으로 처리한다. */
    private MediaType resolveMediaType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }

        try {
            return MediaType.parseMediaType(contentType);
        } catch (IllegalArgumentException ex) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
