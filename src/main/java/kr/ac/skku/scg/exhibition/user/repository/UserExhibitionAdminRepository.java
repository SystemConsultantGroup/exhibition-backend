package kr.ac.skku.scg.exhibition.user.repository;

import java.util.UUID;
import kr.ac.skku.scg.exhibition.user.domain.UserExhibitionAdminEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserExhibitionAdminRepository extends JpaRepository<UserExhibitionAdminEntity, UUID> {

    boolean existsByUser_IdAndExhibition_Id(UUID userId, UUID exhibitionId);

    void deleteAllByExhibition_Id(UUID exhibitionId);
}
