package kr.ac.skku.scg.exhibition.eventperiod.service;

import java.util.List;
import java.util.UUID;
import kr.ac.skku.scg.exhibition.eventperiod.domain.EventPeriodEntity;
import kr.ac.skku.scg.exhibition.eventperiod.dto.request.AdminEventPeriodRequest;
import kr.ac.skku.scg.exhibition.eventperiod.dto.request.EventPeriodListRequest;
import kr.ac.skku.scg.exhibition.eventperiod.dto.response.EventPeriodResponse;
import kr.ac.skku.scg.exhibition.eventperiod.repository.EventPeriodRepository;
import kr.ac.skku.scg.exhibition.exhibition.domain.ExhibitionEntity;
import kr.ac.skku.scg.exhibition.global.auth.resolver.AuthenticatedUser;
import kr.ac.skku.scg.exhibition.global.auth.service.AdminAuthorizationService;
import kr.ac.skku.scg.exhibition.global.error.ConflictException;
import kr.ac.skku.scg.exhibition.global.error.NotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EventPeriodService {
    private final EventPeriodRepository repository;
    private final AdminAuthorizationService authorizationService;

    public EventPeriodService(EventPeriodRepository repository, AdminAuthorizationService authorizationService) {
        this.repository = repository;
        this.authorizationService = authorizationService;
    }

    /** 발표회차 하나를 조회한다. */
    public EventPeriodResponse get(UUID id, UUID exhibitionId) { return toResponse(find(id, exhibitionId)); }

    /** 발표회차 목록을 조회한다. */
    public List<EventPeriodResponse> list(EventPeriodListRequest request) {
        return repository.findAllByExhibition_IdOrderByCreatedAtDesc(request.getExhibitionId()).stream().map(this::toResponse).toList();
    }

    /** 발표회차를 생성한다. */
    @Transactional
    public EventPeriodResponse create(ExhibitionEntity exhibition, AdminEventPeriodRequest request, AuthenticatedUser user) {
        authorizationService.requireExhibitionAdmin(user, exhibition.getId());
        validate(request);
        return toResponse(repository.save(new EventPeriodEntity(null, exhibition, request.name().trim(), request.startTime(), request.endTime())));
    }

    /** 발표회차를 수정한다. */
    @Transactional
    public EventPeriodResponse update(UUID id, UUID exhibitionId, AdminEventPeriodRequest request, AuthenticatedUser user) {
        authorizationService.requireExhibitionAdmin(user, exhibitionId);
        validate(request);
        EventPeriodEntity entity = find(id, exhibitionId);
        entity.update(request.name().trim(), request.startTime(), request.endTime());
        return toResponse(entity);
    }

    /** 참조 데이터가 없는 발표회차를 삭제한다. */
    @Transactional
    public void delete(UUID id, UUID exhibitionId, AuthenticatedUser user) {
        authorizationService.requireExhibitionAdmin(user, exhibitionId);
        try {
            repository.delete(find(id, exhibitionId));
            repository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("Event period is referenced and cannot be deleted: " + id);
        }
    }

    /** 기간 입력을 검증한다. */
    private void validate(AdminEventPeriodRequest request) {
        if (!request.startTime().isBefore(request.endTime())) throw new IllegalArgumentException("startTime must be before endTime");
    }

    /** 전시회 범위에서 발표회차를 찾는다. */
    private EventPeriodEntity find(UUID id, UUID exhibitionId) {
        return repository.findByIdAndExhibition_Id(id, exhibitionId)
                .orElseThrow(() -> new NotFoundException("Event period not found: " + id));
    }

    /** 발표회차 응답을 만든다. */
    private EventPeriodResponse toResponse(EventPeriodEntity e) {
        return new EventPeriodResponse(e.getId(), e.getExhibition().getId(), e.getName(), e.getStartTime(), e.getEndTime());
    }
}
