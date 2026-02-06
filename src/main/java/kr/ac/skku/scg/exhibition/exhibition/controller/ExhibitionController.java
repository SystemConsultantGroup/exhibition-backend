package kr.ac.skku.scg.exhibition.exhibition.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import kr.ac.skku.scg.exhibition.exhibition.dto.request.ExhibitionListRequest;
import kr.ac.skku.scg.exhibition.exhibition.dto.response.ExhibitionResponse;
import kr.ac.skku.scg.exhibition.exhibition.service.ExhibitionService;
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
@RequestMapping("/exhibitions")
@RequiredArgsConstructor
public class ExhibitionController {

    private final ExhibitionService exhibitionService;

    @GetMapping("/{id}")
    public ResponseEntity<ExhibitionResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(exhibitionService.get(id));
    }

    @GetMapping
    public ResponseEntity<ListResponse<ExhibitionResponse>> list(@Valid @ModelAttribute ExhibitionListRequest request) {
        List<ExhibitionResponse> items = exhibitionService.list(request);
        return ResponseEntity.ok(ListResponse.of(items));
    }
}
