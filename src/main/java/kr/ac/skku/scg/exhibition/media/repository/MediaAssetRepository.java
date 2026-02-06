package kr.ac.skku.scg.exhibition.media.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import kr.ac.skku.scg.exhibition.media.domain.MediaAssetEntity;

public interface MediaAssetRepository extends JpaRepository<MediaAssetEntity, UUID> {

    List<MediaAssetEntity> findByItemIdOrderByCreatedAtAsc(UUID itemId);

    Optional<MediaAssetEntity> findByIdAndItemId(UUID id, UUID itemId);
}
