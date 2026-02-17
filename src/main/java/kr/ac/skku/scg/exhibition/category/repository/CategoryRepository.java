package kr.ac.skku.scg.exhibition.category.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import kr.ac.skku.scg.exhibition.category.domain.CategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<CategoryEntity, UUID> {

    List<CategoryEntity> findAllByExhibition_Id(UUID exhibitionId);

    Optional<CategoryEntity> findByExhibition_IdAndName(UUID exhibitionId, String name);
}
