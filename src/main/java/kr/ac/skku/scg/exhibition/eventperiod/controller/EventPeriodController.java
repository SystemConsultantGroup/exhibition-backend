package kr.ac.skku.scg.exhibition.eventperiod.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import kr.ac.skku.scg.exhibition.eventperiod.dto.request.EventPeriodListRequest;
import kr.ac.skku.scg.exhibition.eventperiod.dto.response.EventPeriodResponse;
import kr.ac.skku.scg.exhibition.eventperiod.service.EventPeriodService;
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
@RequestMapping("/event-periods")
@RequiredArgsConstructor
public class EventPeriodController {

    private final EventPeriodService eventPeriodService;

    @GetMapping("/{id}")
    public ResponseEntity<EventPeriodResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(eventPeriodService.get(id));
    }

    @GetMapping
    public ResponseEntity<ListResponse<EventPeriodResponse>> list(@Valid @ModelAttribute EventPeriodListRequest request) {
        List<EventPeriodResponse> items = eventPeriodService.list(request);
        return ResponseEntity.ok(ListResponse.of(items));
    }
}
