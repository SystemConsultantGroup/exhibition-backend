package kr.ac.skku.scg.exhibition.eventperiod.service;

import java.util.List;
import java.util.UUID;
import kr.ac.skku.scg.exhibition.eventperiod.domain.EventPeriodEntity;
import kr.ac.skku.scg.exhibition.eventperiod.dto.request.EventPeriodListRequest;
import kr.ac.skku.scg.exhibition.eventperiod.dto.response.EventPeriodResponse;
import kr.ac.skku.scg.exhibition.eventperiod.repository.EventPeriodRepository;
import kr.ac.skku.scg.exhibition.global.error.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EventPeriodService {

    private final EventPeriodRepository eventPeriodRepository;

    public EventPeriodService(EventPeriodRepository eventPeriodRepository) {
        this.eventPeriodRepository = eventPeriodRepository;
    }

    public EventPeriodResponse get(UUID id) {
        EventPeriodEntity eventPeriod = eventPeriodRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Event period not found: " + id));
        return toResponse(eventPeriod);
    }

    public List<EventPeriodResponse> list(EventPeriodListRequest request) {
        return eventPeriodRepository.findAllByExhibition_Id(request.getExhibitionId()).stream()
                .map(this::toResponse)
                .toList();
    }

    private EventPeriodResponse toResponse(EventPeriodEntity eventPeriod) {
        return new EventPeriodResponse(
                eventPeriod.getId(),
                eventPeriod.getExhibition().getId(),
                eventPeriod.getName(),
                eventPeriod.getStartTime(),
                eventPeriod.getEndTime(),
                eventPeriod.getCreatedAt(),
                eventPeriod.getUpdatedAt());
    }
}
