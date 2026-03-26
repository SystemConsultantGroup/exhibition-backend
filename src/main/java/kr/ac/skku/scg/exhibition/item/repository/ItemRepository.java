package kr.ac.skku.scg.exhibition.item.repository;

import java.util.UUID;
import kr.ac.skku.scg.exhibition.item.domain.ItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRepository extends JpaRepository<ItemEntity, UUID>, ItemRepositoryCustom {

    java.util.Optional<ItemEntity> findByIdAndExhibition_Id(UUID id, UUID exhibitionId);

    boolean existsByIdAndExhibition_Id(UUID id, UUID exhibitionId);
}
