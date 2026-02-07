package kr.ac.skku.scg.exhibition.board.repository;

import java.util.List;
import java.util.UUID;
import kr.ac.skku.scg.exhibition.board.domain.BoardEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardRepository extends JpaRepository<BoardEntity, UUID> {

    List<BoardEntity> findAllByExhibition_Id(UUID exhibitionId);
}
