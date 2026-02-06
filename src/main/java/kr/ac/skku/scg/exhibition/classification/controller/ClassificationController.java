package kr.ac.skku.scg.exhibition.classification.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import kr.ac.skku.scg.exhibition.classification.dto.request.ClassificationListRequest;
import kr.ac.skku.scg.exhibition.classification.dto.response.ClassificationResponse;
import kr.ac.skku.scg.exhibition.classification.service.ClassificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/classifications")
public class ClassificationController {

    private final ClassificationService classificationService;

    public ClassificationController(ClassificationService classificationService) {
        this.classificationService = classificationService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClassificationResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(classificationService.get(id));
    }

    @GetMapping
    public ResponseEntity<List<ClassificationResponse>> list(@Valid @ModelAttribute ClassificationListRequest request) {
        return ResponseEntity.ok(classificationService.list(request));
    }
}
