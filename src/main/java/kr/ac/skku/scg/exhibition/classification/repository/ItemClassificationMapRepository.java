package kr.ac.skku.scg.exhibition.classification.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import kr.ac.skku.scg.exhibition.classification.domain.ItemClassificationMapEntity;

public interface ItemClassificationMapRepository extends JpaRepository<ItemClassificationMapEntity, UUID> {

    List<ItemClassificationMapEntity> findByItemId(UUID itemId);

    boolean existsByItemIdAndClassificationId(UUID itemId, UUID classificationId);

    void deleteByClassificationId(UUID classificationId);
}
