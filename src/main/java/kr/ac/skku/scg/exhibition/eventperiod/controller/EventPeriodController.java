package kr.ac.skku.scg.exhibition.eventperiod.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import kr.ac.skku.scg.exhibition.eventperiod.dto.request.EventPeriodListRequest;
import kr.ac.skku.scg.exhibition.eventperiod.dto.response.EventPeriodResponse;
import kr.ac.skku.scg.exhibition.eventperiod.service.EventPeriodService;
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
public class EventPeriodController {

    private final EventPeriodService eventPeriodService;

    public EventPeriodController(EventPeriodService eventPeriodService) {
        this.eventPeriodService = eventPeriodService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventPeriodResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(eventPeriodService.get(id));
    }

    @GetMapping
    public ResponseEntity<List<EventPeriodResponse>> list(@Valid @ModelAttribute EventPeriodListRequest request) {
        return ResponseEntity.ok(eventPeriodService.list(request));
    }
}
