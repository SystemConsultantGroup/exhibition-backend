package kr.ac.skku.scg.exhibition.media.controller;

import static kr.ac.skku.scg.exhibition.media.dto.MediaDtos.*;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import kr.ac.skku.scg.exhibition.media.service.MediaService;
import kr.ac.skku.scg.exhibition.media.domain.MediaAssetEntity;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping
public class MediaController {

    private final MediaService mediaService;

    @GetMapping("/items/{itemId}/media")
    public ResponseEntity<List<MediaResponse>> listByItem(@PathVariable UUID itemId) {
        var response = mediaService.listByItem(itemId).stream()
            .map(this::toResponse)
            .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/media/{mediaId}")
    public ResponseEntity<byte[]> getMedia(@PathVariable UUID mediaId) {
        var object = mediaService.getObject(mediaId);
        var media = object.media();

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(media.getMediaType()))
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + media.getId() + "\"")
            .contentLength(object.bytes().length)
            .body(object.bytes());
    }

    private MediaResponse toResponse(MediaAssetEntity entity) {
        return new MediaResponse(
            entity.getId(),
            entity.getItem().getId(),
            entity.getExhibition().getId(),
            entity.getObjectKey(),
            entity.getMediaType(),
            entity.getSize(),
            entity.getCreatedAt()
        );
    }
}
