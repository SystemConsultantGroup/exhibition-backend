package kr.ac.skku.scg.exhibition.eventperiod.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import kr.ac.skku.scg.exhibition.eventperiod.domain.EventPeriodEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventPeriodRepository extends JpaRepository<EventPeriodEntity, UUID> {

    List<EventPeriodEntity> findAllByExhibition_Id(UUID exhibitionId);

    Optional<EventPeriodEntity> findByIdAndExhibition_Id(UUID id, UUID exhibitionId);
}
