package kr.ac.skku.scg.exhibition.classification.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import kr.ac.skku.scg.exhibition.classification.domain.ItemClassificationEntity;

public interface ItemClassificationRepository extends JpaRepository<ItemClassificationEntity, UUID> {

    List<ItemClassificationEntity> findByExhibitionIdOrderByNameAsc(UUID exhibitionId);

    Optional<ItemClassificationEntity> findByExhibitionIdAndName(UUID exhibitionId, String name);
}
