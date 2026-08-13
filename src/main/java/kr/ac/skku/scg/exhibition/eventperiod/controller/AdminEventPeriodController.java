package kr.ac.skku.scg.exhibition.eventperiod.controller;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import kr.ac.skku.scg.exhibition.eventperiod.dto.request.AdminEventPeriodRequest;
import kr.ac.skku.scg.exhibition.eventperiod.dto.response.EventPeriodResponse;
import kr.ac.skku.scg.exhibition.eventperiod.service.EventPeriodService;
import kr.ac.skku.scg.exhibition.exhibition.domain.ExhibitionEntity;
import kr.ac.skku.scg.exhibition.global.auth.resolver.AuthenticatedUser;
import kr.ac.skku.scg.exhibition.global.auth.resolver.CurrentUser;
import kr.ac.skku.scg.exhibition.global.tenant.CurrentExhibition;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/event-periods")
@RequiredArgsConstructor
public class AdminEventPeriodController {
    private final EventPeriodService service;

    /** 발표회차를 생성한다. */
    @PostMapping
    public ResponseEntity<EventPeriodResponse> create(@Valid @RequestBody AdminEventPeriodRequest request,
            @CurrentExhibition ExhibitionEntity exhibition, @CurrentUser AuthenticatedUser user) {
        EventPeriodResponse response = service.create(exhibition, request, user);
        return ResponseEntity.created(URI.create("/admin/event-periods/" + response.id())).body(response);
    }

    /** 발표회차를 수정한다. */
    @PutMapping("/{id}")
    public ResponseEntity<EventPeriodResponse> update(@PathVariable UUID id,
            @Valid @RequestBody AdminEventPeriodRequest request, @CurrentExhibition ExhibitionEntity exhibition,
            @CurrentUser AuthenticatedUser user) {
        return ResponseEntity.ok(service.update(id, exhibition.getId(), request, user));
    }

    /** 발표회차를 삭제한다. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id, @CurrentExhibition ExhibitionEntity exhibition,
            @CurrentUser AuthenticatedUser user) {
        service.delete(id, exhibition.getId(), user);
        return ResponseEntity.noContent().build();
    }
}
