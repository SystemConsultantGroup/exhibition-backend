package kr.ac.skku.scg.exhibition.media.repository;

import java.util.List;
import java.util.UUID;
import kr.ac.skku.scg.exhibition.media.domain.MediaAssetEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaAssetRepository extends JpaRepository<MediaAssetEntity, UUID> {

    List<MediaAssetEntity> findAllByExhibition_Id(UUID exhibitionId);

    List<MediaAssetEntity> findAllByExhibition_IdAndItem_Id(UUID exhibitionId, UUID itemId);
}
