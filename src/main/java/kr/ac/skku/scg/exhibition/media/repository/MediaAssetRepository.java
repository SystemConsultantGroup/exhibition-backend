package kr.ac.skku.scg.exhibition.media.repository;

import java.util.List;
import java.util.UUID;
import kr.ac.skku.scg.exhibition.media.domain.MediaAssetEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaAssetRepository extends JpaRepository<MediaAssetEntity, UUID> {

    List<MediaAssetEntity> findAllByItem_Id(UUID itemId);

    void deleteAllByItem_Id(UUID itemId);

    List<MediaAssetEntity> findAllByBoard_Id(UUID boardId);

    void deleteAllByBoard_Id(UUID boardId);
}
