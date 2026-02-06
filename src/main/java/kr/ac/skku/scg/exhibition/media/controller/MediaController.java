package kr.ac.skku.scg.exhibition.media.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import kr.ac.skku.scg.exhibition.media.dto.request.MediaListRequest;
import kr.ac.skku.scg.exhibition.media.dto.response.MediaFileResponse;
import kr.ac.skku.scg.exhibition.media.dto.response.MediaResponse;
import kr.ac.skku.scg.exhibition.media.service.MediaService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/media")
public class MediaController {

    private final MediaService mediaService;

    public MediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<MediaResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(mediaService.getMetadata(id));
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<ByteArrayResource> getFile(@PathVariable UUID id) {
        MediaFileResponse file = mediaService.getFile(id);
        ByteArrayResource resource = new ByteArrayResource(file.bytes());
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (file.contentType() != null && !file.contentType().isBlank()) {
            mediaType = MediaType.parseMediaType(file.contentType());
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(file.contentLength())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(file.fileName()).build().toString())
                .body(resource);
    }

    @GetMapping
    public ResponseEntity<List<MediaResponse>> list(@Valid @ModelAttribute MediaListRequest request) {
        return ResponseEntity.ok(mediaService.list(request));
    }
}
