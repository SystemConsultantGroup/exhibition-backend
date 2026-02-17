package kr.ac.skku.scg.exhibition.classification.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import kr.ac.skku.scg.exhibition.classification.domain.ItemClassificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemClassificationRepository extends JpaRepository<ItemClassificationEntity, UUID> {

    List<ItemClassificationEntity> findAllByExhibition_Id(UUID exhibitionId);

    Optional<ItemClassificationEntity> findByExhibition_IdAndName(UUID exhibitionId, String name);
}
