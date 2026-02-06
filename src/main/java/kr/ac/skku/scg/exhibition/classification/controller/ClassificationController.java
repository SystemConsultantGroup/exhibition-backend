package kr.ac.skku.scg.exhibition.classification.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import kr.ac.skku.scg.exhibition.classification.dto.request.ClassificationListRequest;
import kr.ac.skku.scg.exhibition.classification.dto.response.ClassificationResponse;
import kr.ac.skku.scg.exhibition.classification.service.ClassificationService;
import kr.ac.skku.scg.exhibition.global.dto.ListResponse;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class ClassificationController {

    private final ClassificationService classificationService;

    @GetMapping("/{id}")
    public ResponseEntity<ClassificationResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(classificationService.get(id));
    }

    @GetMapping
    public ResponseEntity<ListResponse<ClassificationResponse>> list(@Valid @ModelAttribute ClassificationListRequest request) {
        List<ClassificationResponse> items = classificationService.list(request);
        return ResponseEntity.ok(ListResponse.of(items));
    }
}
