package kr.ac.skku.scg.exhibition.board.service;

import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import java.util.List;
import java.util.UUID;
import kr.ac.skku.scg.exhibition.board.domain.BoardEntity;
import kr.ac.skku.scg.exhibition.board.dto.request.AdminBoardRequest;
import kr.ac.skku.scg.exhibition.board.dto.request.BoardListRequest;
import kr.ac.skku.scg.exhibition.board.dto.response.AttachmentMediaResponse;
import kr.ac.skku.scg.exhibition.board.dto.response.BoardResponse;
import kr.ac.skku.scg.exhibition.board.repository.BoardRepository;
import kr.ac.skku.scg.exhibition.exhibition.domain.ExhibitionEntity;
import kr.ac.skku.scg.exhibition.global.auth.resolver.AuthenticatedUser;
import kr.ac.skku.scg.exhibition.global.auth.service.AdminAuthorizationService;
import kr.ac.skku.scg.exhibition.global.config.MinioProperties;
import kr.ac.skku.scg.exhibition.global.error.NotFoundException;
import kr.ac.skku.scg.exhibition.media.domain.MediaAssetEntity;
import kr.ac.skku.scg.exhibition.media.repository.MediaAssetRepository;
import kr.ac.skku.scg.exhibition.user.domain.UserEntity;
import kr.ac.skku.scg.exhibition.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@Transactional(readOnly = true)
public class BoardService {

    private final BoardRepository boardRepository;
    private final UserRepository userRepository;
    private final MediaAssetRepository mediaAssetRepository;
    private final AdminAuthorizationService adminAuthorizationService;
    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    public BoardService(
            BoardRepository boardRepository,
            UserRepository userRepository,
            MediaAssetRepository mediaAssetRepository,
            AdminAuthorizationService adminAuthorizationService,
            MinioClient minioClient,
            MinioProperties minioProperties
    ) {
        this.boardRepository = boardRepository;
        this.userRepository = userRepository;
        this.mediaAssetRepository = mediaAssetRepository;
        this.adminAuthorizationService = adminAuthorizationService;
        this.minioClient = minioClient;
        this.minioProperties = minioProperties;
    }

    /** 전시회의 공지사항 하나를 조회한다. */
    public BoardResponse get(UUID id, UUID exhibitionId) {
        BoardEntity board = boardRepository.findDetailByIdAndExhibitionId(id, exhibitionId)
                .orElseThrow(() -> new NotFoundException("Board not found: " + id));
        return toResponse(board);
    }

    /** 전시회의 공지사항 목록을 조회한다. */
    public List<BoardResponse> list(BoardListRequest request) {
        return boardRepository.findAllDetailByExhibitionId(request.getExhibitionId()).stream()
                .map(this::toResponse)
                .toList();
    }

    /** 현재 관리자를 작성자로 하여 공지사항을 생성한다. */
    @Transactional
    public BoardResponse create(ExhibitionEntity exhibition, AdminBoardRequest request, AuthenticatedUser user) {
        adminAuthorizationService.requireExhibitionAdmin(user, exhibition.getId());
        UserEntity author = userRepository.findById(user.id())
                .orElseThrow(() -> new NotFoundException("User not found: " + user.id()));
        BoardEntity board = new BoardEntity(
                null, exhibition, request.title().trim(), request.content(), List.of(), author);
        return toResponse(boardRepository.save(board));
    }

    /** 현재 전시회에 속한 공지사항의 제목과 내용만 수정한다. */
    @Transactional
    public BoardResponse update(UUID id, UUID exhibitionId, AdminBoardRequest request, AuthenticatedUser user) {
        adminAuthorizationService.requireExhibitionAdmin(user, exhibitionId);
        BoardEntity board = findDetail(id, exhibitionId);
        board.update(request.title().trim(), request.content());
        return toResponse(board);
    }

    /** 현재 전시회의 공지사항과 해당 공지가 소유한 첨부 미디어만 삭제한다. */
    @Transactional
    public void delete(UUID id, UUID exhibitionId, AuthenticatedUser user) {
        adminAuthorizationService.requireExhibitionAdmin(user, exhibitionId);
        BoardEntity board = boardRepository.findByIdAndExhibition_Id(id, exhibitionId)
                .orElseThrow(() -> new NotFoundException("Board not found: " + id));
        List<String> objectKeys = mediaAssetRepository.findAllByBoard_Id(id).stream()
                .map(MediaAssetEntity::getObjectKey)
                .toList();
        mediaAssetRepository.deleteAllByBoard_Id(id);
        mediaAssetRepository.flush();
        boardRepository.delete(board);
        removeAfterCommit(objectKeys);
    }

    /** 전시회 범위에서 첨부 정보를 포함한 공지사항을 찾는다. */
    private BoardEntity findDetail(UUID id, UUID exhibitionId) {
        return boardRepository.findDetailByIdAndExhibitionId(id, exhibitionId)
                .orElseThrow(() -> new NotFoundException("Board not found: " + id));
    }

    /** 공지사항 엔티티를 API 응답으로 변환한다. */
    private BoardResponse toResponse(BoardEntity board) {
        List<UUID> attachmentMediaIds = board.getAttachmentMediaList().stream()
                .map(MediaAssetEntity::getId)
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

    /** 첨부 미디어 객체 키에서 원본 파일명을 추출한다. */
    private String extractFileName(MediaAssetEntity media) {
        String objectKey = media.getObjectKey();
        int index = objectKey.lastIndexOf('/');
        return index >= 0 ? objectKey.substring(index + 1) : objectKey;
    }

    /** DB 커밋 후 첨부 미디어의 MinIO 객체를 최선 노력으로 삭제한다. */
    private void removeAfterCommit(List<String> objectKeys) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                for (String objectKey : objectKeys) {
                    try {
                        minioClient.removeObject(RemoveObjectArgs.builder()
                                .bucket(minioProperties.getBucket())
                                .object(objectKey)
                                .build());
                    } catch (Exception ignored) {
                        // Best effort cleanup after the database commit.
                    }
                }
            }
        });
    }
}
