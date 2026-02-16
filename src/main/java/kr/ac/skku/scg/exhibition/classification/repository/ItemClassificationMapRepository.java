package kr.ac.skku.scg.exhibition.classification.repository;

import java.util.UUID;
import kr.ac.skku.scg.exhibition.classification.domain.ItemClassificationMapEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemClassificationMapRepository extends JpaRepository<ItemClassificationMapEntity, UUID> {
}
