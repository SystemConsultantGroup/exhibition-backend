package kr.ac.skku.scg.exhibition.classification.controller;

import static kr.ac.skku.scg.exhibition.classification.dto.ClassificationDtos.*;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import kr.ac.skku.scg.exhibition.classification.service.ClassificationService;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping
public class ClassificationController {

    private final ClassificationService classificationService;

    @GetMapping("/exhibitions/{exhibitionId}/classifications")
    public List<ClassificationResponse> list(@PathVariable UUID exhibitionId) {
        return classificationService.list(exhibitionId).stream()
            .map(c -> new ClassificationResponse(c.getId(), c.getExhibition().getId(), c.getName()))
            .toList();
    }
}
