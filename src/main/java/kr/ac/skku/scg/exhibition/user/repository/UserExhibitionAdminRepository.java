package kr.ac.skku.scg.exhibition.user.repository;

import java.util.List;
import java.util.UUID;
import kr.ac.skku.scg.exhibition.exhibition.domain.ExhibitionEntity;
import kr.ac.skku.scg.exhibition.user.domain.UserExhibitionAdminEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserExhibitionAdminRepository extends JpaRepository<UserExhibitionAdminEntity, UUID> {

    boolean existsByUser_IdAndExhibition_Id(UUID userId, UUID exhibitionId);

    @Query("""
            select assignment.exhibition from UserExhibitionAdminEntity assignment
            where assignment.user.id = :userId
            order by assignment.exhibition.name
            """)
    List<ExhibitionEntity> findExhibitionsByUserId(@Param("userId") UUID userId);

    void deleteAllByExhibition_Id(UUID exhibitionId);
}
