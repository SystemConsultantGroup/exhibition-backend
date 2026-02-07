package kr.ac.skku.scg.exhibition.media.controller;

import java.util.UUID;
import kr.ac.skku.scg.exhibition.media.dto.response.MediaFileResponse;
import kr.ac.skku.scg.exhibition.media.service.MediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
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

    private final MediaService mediaService;

    @GetMapping("/{id}")
    public ResponseEntity<ByteArrayResource> get(@PathVariable UUID id) {
        MediaFileResponse file = mediaService.getFile(id);
        ByteArrayResource resource = new ByteArrayResource(file.bytes());
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (file.contentType() != null && !file.contentType().isBlank()) {
            mediaType = MediaType.parseMediaType(file.contentType());
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(file.contentLength())
                .body(resource);
    }
}
