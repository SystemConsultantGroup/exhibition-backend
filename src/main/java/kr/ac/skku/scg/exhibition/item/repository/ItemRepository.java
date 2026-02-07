package kr.ac.skku.scg.exhibition.item.repository;

import java.util.List;
import java.util.UUID;
import kr.ac.skku.scg.exhibition.item.domain.ItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRepository extends JpaRepository<ItemEntity, UUID> {

    List<ItemEntity> findAllByExhibition_Id(UUID exhibitionId);

    List<ItemEntity> findAllByExhibition_IdAndCategory_Id(UUID exhibitionId, UUID categoryId);

    List<ItemEntity> findAllByExhibition_IdAndEventPeriod_Id(UUID exhibitionId, UUID eventPeriodId);
}
