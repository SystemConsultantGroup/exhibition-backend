package kr.ac.skku.scg.exhibition.media.repository;

import java.util.UUID;
import kr.ac.skku.scg.exhibition.media.domain.MediaAssetEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaAssetRepository extends JpaRepository<MediaAssetEntity, UUID> {
}
