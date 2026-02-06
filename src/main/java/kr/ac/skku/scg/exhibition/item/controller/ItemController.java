package kr.ac.skku.scg.exhibition.item.controller;

import static kr.ac.skku.scg.exhibition.item.dto.ItemDtos.*;

import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import kr.ac.skku.scg.exhibition.global.dto.PageResponse;
import kr.ac.skku.scg.exhibition.item.service.ItemService;
import kr.ac.skku.scg.exhibition.item.domain.ItemEntity;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping
public class ItemController {

    private final ItemService itemService;

    @GetMapping("/exhibitions/{exhibitionId}/items")
    public ResponseEntity<PageResponse<ItemResponse>> list(
        @PathVariable UUID exhibitionId,
        @RequestParam(name = "category_id", required = false) UUID categoryId,
        @RequestParam(required = false) String q,
        @RequestParam(required = false) String visibility,
        @RequestParam(required = false) Boolean published,
        @RequestParam(required = false) String classification,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(name = "page_size", defaultValue = "20") int pageSize
    ) {
        var pageable = PageRequest.of(Math.max(0, page - 1), Math.max(1, pageSize), Sort.by("createdAt").descending());
        var result = itemService.list(exhibitionId, categoryId, q, visibility, published, classification, pageable);
        var response = new PageResponse<>(result.getContent().stream().map(this::toResponse).toList(), page, pageSize, result.getTotalElements());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/items/{itemId}")
    public ResponseEntity<ItemResponse> get(@PathVariable UUID itemId) {
        return ResponseEntity.ok(toResponse(itemService.get(itemId)));
    }

    private ItemResponse toResponse(ItemEntity item) {
        return new ItemResponse(
            item.getId(),
            item.getExhibition().getId(),
            item.getCategory().getId(),
            item.getTitle(),
            item.getSummary(),
            item.getDescription(),
            item.getAuthorName(),
            item.getAuthorEmail(),
            item.getVisibility().name().toLowerCase(),
            item.getPublishedAt()
        );
    }
}
