package kr.ac.skku.scg.exhibition.exhibition.controller;

import static kr.ac.skku.scg.exhibition.exhibition.dto.ExhibitionDtos.*;

import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import kr.ac.skku.scg.exhibition.exhibition.service.ExhibitionService;
import kr.ac.skku.scg.exhibition.exhibition.domain.ExhibitionServiceEntity;
import kr.ac.skku.scg.exhibition.global.dto.PageResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/exhibitions")
@RequiredArgsConstructor
public class ExhibitionController {

    private final ExhibitionService exhibitionService;

    @GetMapping
    public PageResponse<ExhibitionResponse> list(
        @RequestParam(required = false) Boolean active,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(name = "page_size", defaultValue = "20") int pageSize
    ) {
        var pageable = PageRequest.of(Math.max(0, page - 1), Math.max(1, pageSize), Sort.by("createdAt").descending());
        var result = exhibitionService.list(active, pageable);
        return new PageResponse<>(result.getContent().stream().map(this::toResponse).toList(), page, pageSize, result.getTotalElements());
    }

    @GetMapping("/{exhibitionId}")
    public ExhibitionResponse get(@PathVariable UUID exhibitionId) {
        return toResponse(exhibitionService.get(exhibitionId));
    }

    private ExhibitionResponse toResponse(ExhibitionServiceEntity entity) {
        return new ExhibitionResponse(
            entity.getId(),
            entity.getSlug(),
            entity.getName(),
            entity.getDescription(),
            entity.getStartDate(),
            entity.getEndDate(),
            entity.isActive(),
            entity.isPopupEnabled(),
            entity.getPopupImageUrl(),
            entity.getIntroTitle(),
            entity.getIntroDescription(),
            entity.getIntroVideoUrl()
        );
    }
}
