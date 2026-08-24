package kr.ac.skku.scg.exhibition.board.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import kr.ac.skku.scg.exhibition.board.domain.BoardEntity;
import kr.ac.skku.scg.exhibition.board.dto.request.AdminBoardRequest;
import kr.ac.skku.scg.exhibition.board.dto.response.BoardResponse;
import kr.ac.skku.scg.exhibition.board.repository.BoardRepository;
import kr.ac.skku.scg.exhibition.exhibition.domain.ExhibitionEntity;
import kr.ac.skku.scg.exhibition.global.auth.resolver.AuthenticatedUser;
import kr.ac.skku.scg.exhibition.global.auth.service.AdminAuthorizationService;
import kr.ac.skku.scg.exhibition.global.config.MinioProperties;
import kr.ac.skku.scg.exhibition.global.error.ForbiddenException;
import kr.ac.skku.scg.exhibition.global.error.NotFoundException;
import kr.ac.skku.scg.exhibition.media.domain.MediaAssetEntity;
import kr.ac.skku.scg.exhibition.media.repository.MediaAssetRepository;
import kr.ac.skku.scg.exhibition.user.domain.UserEntity;
import kr.ac.skku.scg.exhibition.user.domain.UserType;
import kr.ac.skku.scg.exhibition.user.repository.UserExhibitionAdminRepository;
import kr.ac.skku.scg.exhibition.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class BoardServiceTest {

    @Mock private BoardRepository boardRepository;
    @Mock private UserRepository userRepository;
    @Mock private UserExhibitionAdminRepository assignmentRepository;
    @Mock private MediaAssetRepository mediaAssetRepository;
    @Mock private MinioClient minioClient;
    private BoardService boardService;

    @BeforeEach
    void setUp() {
        MinioProperties properties = new MinioProperties();
        properties.setBucket("exhibition");
        boardService = new BoardService(
                boardRepository,
                userRepository,
                mediaAssetRepository,
                new AdminAuthorizationService(assignmentRepository),
                minioClient,
                properties
        );
    }

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void create_allowsAdminAndUsesAuthenticatedUserAsAuthor() {
        UUID exhibitionId = UUID.randomUUID();
        ExhibitionEntity exhibition = exhibition(exhibitionId);
        AuthenticatedUser admin = user(UserType.ADMIN);
        UserEntity author = author(admin.id(), UserType.ADMIN);
        when(userRepository.findById(admin.id())).thenReturn(Optional.of(author));
        when(boardRepository.save(any(BoardEntity.class))).thenAnswer(invocation -> {
            BoardEntity input = invocation.getArgument(0);
            return board(UUID.randomUUID(), exhibition, input.getTitle(), input.getContent(), author);
        });

        BoardResponse response = boardService.create(exhibition, request(), admin);

        assertThat(response.title()).isEqualTo("공지");
        assertThat(response.authorUserId()).isEqualTo(admin.id());
        verifyNoInteractions(assignmentRepository);
    }

    @Test
    void createAndUpdate_allowAssignedSubAdminAndPreserveAuthor() {
        UUID exhibitionId = UUID.randomUUID();
        UUID boardId = UUID.randomUUID();
        ExhibitionEntity exhibition = exhibition(exhibitionId);
        AuthenticatedUser subAdmin = user(UserType.SUB_ADMIN);
        UserEntity author = author(subAdmin.id(), UserType.SUB_ADMIN);
        BoardEntity existing = board(boardId, exhibition, "기존", "기존 내용", author);
        when(assignmentRepository.existsByUser_IdAndExhibition_Id(subAdmin.id(), exhibitionId)).thenReturn(true);
        when(userRepository.findById(subAdmin.id())).thenReturn(Optional.of(author));
        when(boardRepository.save(any(BoardEntity.class))).thenAnswer(invocation -> {
            BoardEntity input = invocation.getArgument(0);
            return board(UUID.randomUUID(), exhibition, input.getTitle(), input.getContent(), author);
        });
        when(boardRepository.findDetailByIdAndExhibitionId(boardId, exhibitionId)).thenReturn(Optional.of(existing));

        boardService.create(exhibition, request(), subAdmin);
        BoardResponse updated = boardService.update(
                boardId, exhibitionId, new AdminBoardRequest("수정", "수정 내용"), subAdmin);

        assertThat(updated.title()).isEqualTo("수정");
        assertThat(updated.content()).isEqualTo("수정 내용");
        assertThat(updated.authorUserId()).isEqualTo(author.getId());
        verify(assignmentRepository, org.mockito.Mockito.times(2))
                .existsByUser_IdAndExhibition_Id(subAdmin.id(), exhibitionId);
    }

    @Test
    void create_rejectsUnassignedSubAdminBeforeWrite() {
        ExhibitionEntity exhibition = exhibition(UUID.randomUUID());
        AuthenticatedUser subAdmin = user(UserType.SUB_ADMIN);
        when(assignmentRepository.existsByUser_IdAndExhibition_Id(subAdmin.id(), exhibition.getId())).thenReturn(false);

        assertThatThrownBy(() -> boardService.create(exhibition, request(), subAdmin))
                .isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(userRepository, boardRepository, mediaAssetRepository, minioClient);
    }

    @Test
    void updateAndDelete_rejectUnassignedSubAdminBeforeRepositoryAccess() {
        UUID exhibitionId = UUID.randomUUID();
        UUID boardId = UUID.randomUUID();
        AuthenticatedUser subAdmin = user(UserType.SUB_ADMIN);
        when(assignmentRepository.existsByUser_IdAndExhibition_Id(subAdmin.id(), exhibitionId)).thenReturn(false);

        assertThatThrownBy(() -> boardService.update(boardId, exhibitionId, request(), subAdmin))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> boardService.delete(boardId, exhibitionId, subAdmin))
                .isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(boardRepository, mediaAssetRepository, minioClient);
    }

    @Test
    void updateAndDelete_rejectCrossTenantIds() {
        UUID exhibitionId = UUID.randomUUID();
        UUID boardId = UUID.randomUUID();
        AuthenticatedUser admin = user(UserType.ADMIN);
        when(boardRepository.findDetailByIdAndExhibitionId(boardId, exhibitionId)).thenReturn(Optional.empty());
        when(boardRepository.findByIdAndExhibition_Id(boardId, exhibitionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> boardService.update(boardId, exhibitionId, request(), admin))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> boardService.delete(boardId, exhibitionId, admin))
                .isInstanceOf(NotFoundException.class);

        verify(mediaAssetRepository, never()).deleteAllByBoard_Id(any());
        verify(boardRepository, never()).delete(any());
    }

    @Test
    void delete_allowsAssignedSubAdminAndCleansExactMinioObjectAfterCommit() throws Exception {
        UUID exhibitionId = UUID.randomUUID();
        UUID boardId = UUID.randomUUID();
        AuthenticatedUser subAdmin = user(UserType.SUB_ADMIN);
        BoardEntity board = board(
                boardId, exhibition(exhibitionId), "공지", "내용", author(subAdmin.id(), UserType.SUB_ADMIN));
        MediaAssetEntity attachment = org.mockito.Mockito.mock(MediaAssetEntity.class);
        when(assignmentRepository.existsByUser_IdAndExhibition_Id(subAdmin.id(), exhibitionId)).thenReturn(true);
        when(attachment.getObjectKey()).thenReturn("boards/notice.pdf");
        when(boardRepository.findByIdAndExhibition_Id(boardId, exhibitionId)).thenReturn(Optional.of(board));
        when(mediaAssetRepository.findAllByBoard_Id(boardId)).thenReturn(List.of(attachment));
        TransactionSynchronizationManager.initSynchronization();

        boardService.delete(boardId, exhibitionId, subAdmin);

        verify(mediaAssetRepository).deleteAllByBoard_Id(boardId);
        verify(mediaAssetRepository).flush();
        verify(boardRepository).delete(board);
        verify(minioClient, never()).removeObject(any(RemoveObjectArgs.class));
        List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
        assertThat(synchronizations).hasSize(1);
        synchronizations.get(0).afterCommit();
        ArgumentCaptor<RemoveObjectArgs> args = ArgumentCaptor.forClass(RemoveObjectArgs.class);
        verify(minioClient).removeObject(args.capture());
        assertThat(args.getValue().bucket()).isEqualTo("exhibition");
        assertThat(args.getValue().object()).isEqualTo("boards/notice.pdf");
    }

    private AdminBoardRequest request() {
        return new AdminBoardRequest(" 공지 ", "내용");
    }

    private ExhibitionEntity exhibition(UUID id) {
        return new ExhibitionEntity(id, "slug", "전시");
    }

    private BoardEntity board(UUID id, ExhibitionEntity exhibition, String title, String content, UserEntity author) {
        return new BoardEntity(id, exhibition, title, content, List.of(), author);
    }

    private UserEntity author(UUID id, UserType role) {
        return new UserEntity(id, "kakao:" + id, "관리자", "admin@example.com", role);
    }

    private AuthenticatedUser user(UserType role) {
        return new AuthenticatedUser(UUID.randomUUID(), "관리자", role, null, null, null, null, true);
    }
}
