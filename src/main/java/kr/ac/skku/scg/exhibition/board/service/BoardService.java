package kr.ac.skku.scg.exhibition.board.service;

import java.util.List;
import java.util.UUID;
import kr.ac.skku.scg.exhibition.board.domain.BoardEntity;
import kr.ac.skku.scg.exhibition.board.dto.request.BoardListRequest;
import kr.ac.skku.scg.exhibition.board.dto.response.AttachmentMediaResponse;
import kr.ac.skku.scg.exhibition.board.dto.response.BoardResponse;
import kr.ac.skku.scg.exhibition.board.repository.BoardRepository;
import kr.ac.skku.scg.exhibition.global.error.NotFoundException;
import kr.ac.skku.scg.exhibition.media.domain.MediaAssetEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class BoardService {

    private final BoardRepository boardRepository;

    public BoardService(BoardRepository boardRepository) {
        this.boardRepository = boardRepository;
    }

    public BoardResponse get(UUID id) {
        BoardEntity board = boardRepository.findDetailById(id)
                .orElseThrow(() -> new NotFoundException("Board not found: " + id));
        return toResponse(board);
    }

    public List<BoardResponse> list(BoardListRequest request) {
        return boardRepository.findAllDetailByExhibitionId(request.getExhibitionId()).stream()
                .map(this::toResponse)
                .toList();
    }

    private BoardResponse toResponse(BoardEntity board) {
        List<UUID> attachmentMediaIds = board.getAttachmentMediaList().stream()
                .map(media -> media.getId())
                .toList();
        List<AttachmentMediaResponse> attachmentMedias = board.getAttachmentMediaList().stream()
                .map(media -> new AttachmentMediaResponse(media.getId(), extractFileName(media)))
                .toList();
        return new BoardResponse(
                board.getId(),
                board.getExhibition().getId(),
                board.getTitle(),
                board.getContent(),
                attachmentMediaIds,
                attachmentMedias,
                board.getAuthorUser().getId(),
                board.getCreatedAt(),
                board.getUpdatedAt()
        );
    }

    private String extractFileName(MediaAssetEntity media) {
        String objectKey = media.getObjectKey();
        int index = objectKey.lastIndexOf('/');
        return index >= 0 ? objectKey.substring(index + 1) : objectKey;
    }
}
