package kr.ac.skku.scg.exhibition.board.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import kr.ac.skku.scg.exhibition.board.dto.response.BoardResponse;
import kr.ac.skku.scg.exhibition.board.service.BoardService;
import kr.ac.skku.scg.exhibition.exhibition.domain.ExhibitionEntity;
import kr.ac.skku.scg.exhibition.global.auth.resolver.AuthenticatedUser;
import kr.ac.skku.scg.exhibition.global.config.SecurityConfig;
import kr.ac.skku.scg.exhibition.global.config.WebConfig;
import kr.ac.skku.scg.exhibition.global.error.ApiExceptionHandler;
import kr.ac.skku.scg.exhibition.global.error.NotFoundException;
import kr.ac.skku.scg.exhibition.global.tenant.ExhibitionDomainCacheService;
import kr.ac.skku.scg.exhibition.user.domain.UserEntity;
import kr.ac.skku.scg.exhibition.user.domain.UserType;
import kr.ac.skku.scg.exhibition.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AdminBoardController.class)
@Import({SecurityConfig.class, ApiExceptionHandler.class, WebConfig.class})
class AdminBoardControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private BoardService boardService;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private ExhibitionDomainCacheService exhibitionDomainCacheService;

    @Test
    void delegatesCreateUpdateAndDeleteWithResolvedTenantAndUser() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID exhibitionId = UUID.randomUUID();
        UUID boardId = UUID.randomUUID();
        ExhibitionEntity exhibition = new ExhibitionEntity(exhibitionId, "slug", "전시");
        UserEntity admin = admin(userId);
        AuthenticatedUser authenticatedUser = AuthenticatedUser.from(admin);
        BoardResponse response = new BoardResponse(
                boardId, exhibitionId, "공지", "내용", List.of(), List.of(), userId, Instant.now(), Instant.now());
        when(userRepository.findById(userId)).thenReturn(Optional.of(admin));
        when(exhibitionDomainCacheService.findByDomain("example.com")).thenReturn(Optional.of(exhibition));
        when(boardService.create(eq(exhibition), any(), eq(authenticatedUser))).thenReturn(response);
        when(boardService.update(eq(boardId), eq(exhibitionId), any(), eq(authenticatedUser))).thenReturn(response);

        mockMvc.perform(post("/admin/boards").header("Host", "example.com").with(user(userId.toString()))
                        .requestAttr("auth.userId", userId).contentType(MediaType.APPLICATION_JSON).content(body()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/admin/boards/" + boardId))
                .andExpect(jsonPath("$.id").value(boardId.toString()));
        mockMvc.perform(put("/admin/boards/{id}", boardId).header("Host", "example.com").with(user(userId.toString()))
                        .requestAttr("auth.userId", userId).contentType(MediaType.APPLICATION_JSON).content(body()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("공지"));
        mockMvc.perform(delete("/admin/boards/{id}", boardId).header("Host", "example.com")
                        .with(user(userId.toString())).requestAttr("auth.userId", userId))
                .andExpect(status().isNoContent());

        verify(boardService).create(eq(exhibition), any(), eq(authenticatedUser));
        verify(boardService).update(eq(boardId), eq(exhibitionId), any(), eq(authenticatedUser));
        verify(boardService).delete(boardId, exhibitionId, authenticatedUser);
    }

    @Test
    void updateReturnsNotFoundForCrossTenantBoard() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID exhibitionId = UUID.randomUUID();
        UUID boardId = UUID.randomUUID();
        ExhibitionEntity exhibition = new ExhibitionEntity(exhibitionId, "slug", "전시");
        UserEntity admin = admin(userId);
        AuthenticatedUser authenticatedUser = AuthenticatedUser.from(admin);
        when(userRepository.findById(userId)).thenReturn(Optional.of(admin));
        when(exhibitionDomainCacheService.findByDomain("example.com")).thenReturn(Optional.of(exhibition));
        when(boardService.update(eq(boardId), eq(exhibitionId), any(), eq(authenticatedUser)))
                .thenThrow(new NotFoundException("Board not found: " + boardId));

        mockMvc.perform(put("/admin/boards/{id}", boardId).header("Host", "example.com")
                        .with(user(userId.toString())).requestAttr("auth.userId", userId)
                        .contentType(MediaType.APPLICATION_JSON).content(body()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void createRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/admin/boards")
                        .contentType(MediaType.APPLICATION_JSON).content(body()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    private String body() {
        return "{\"title\":\"공지\",\"content\":\"내용\"}";
    }

    private UserEntity admin(UUID id) {
        UserEntity user = new UserEntity(id, "kakao:test", "관리자", "admin@example.com", UserType.ADMIN);
        user.completeRegistration("관리자", "admin@example.com", null, null, null);
        return user;
    }
}
