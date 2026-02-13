package kr.ac.skku.scg.exhibition.board.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import kr.ac.skku.scg.exhibition.board.domain.BoardEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BoardRepository extends JpaRepository<BoardEntity, UUID> {

    @EntityGraph(attributePaths = {"exhibition", "authorUser", "attachmentMediaList"})
    @Query("select b from BoardEntity b where b.id = :id")
    Optional<BoardEntity> findDetailById(@Param("id") UUID id);

    @EntityGraph(attributePaths = {"exhibition", "authorUser", "attachmentMediaList"})
    @Query("select b from BoardEntity b where b.exhibition.id = :exhibitionId")
    List<BoardEntity> findAllDetailByExhibitionId(@Param("exhibitionId") UUID exhibitionId);
}
