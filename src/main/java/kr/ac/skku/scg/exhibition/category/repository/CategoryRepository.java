package kr.ac.skku.scg.exhibition.category.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import kr.ac.skku.scg.exhibition.category.domain.CategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CategoryRepository extends JpaRepository<CategoryEntity, UUID> {
    @Query("""
        SELECT c
        FROM CategoryEntity c
        WHERE c.exhibition.id = :exhibitionId
        ORDER BY c.createdAt ASC
        """)
    List<CategoryEntity> findAllByExhibition_Id(@Param("exhibitionId") UUID exhibitionId);

    Optional<CategoryEntity> findByIdAndExhibition_Id(UUID id, UUID exhibitionId);

    Optional<CategoryEntity> findByExhibition_IdAndName(UUID exhibitionId, String name);

    Optional<CategoryEntity> findByExhibition_IdAndSlug(UUID exhibitionId, String slug);
}
