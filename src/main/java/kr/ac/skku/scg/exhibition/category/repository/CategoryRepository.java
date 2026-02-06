package kr.ac.skku.scg.exhibition.category.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import kr.ac.skku.scg.exhibition.category.domain.CategoryEntity;

public interface CategoryRepository extends JpaRepository<CategoryEntity, UUID> {

    List<CategoryEntity> findByExhibitionIdOrderByDepthAscOrderIndexAscNameAsc(UUID exhibitionId);

    List<CategoryEntity> findByParentIdOrderByOrderIndexAscNameAsc(UUID parentId);

    Optional<CategoryEntity> findByExhibitionIdAndParentIdAndName(UUID exhibitionId, UUID parentId, String name);

    List<CategoryEntity> findByExhibitionIdAndPathStartingWith(UUID exhibitionId, String pathPrefix);

    List<CategoryEntity> findByIdIn(Collection<UUID> ids);

    Optional<CategoryEntity> findByExhibitionIdAndParentIsNullAndName(UUID exhibitionId, String name);
}
