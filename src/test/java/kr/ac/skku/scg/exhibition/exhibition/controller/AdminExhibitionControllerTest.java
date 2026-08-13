package kr.ac.skku.scg.exhibition.exhibition.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;
import java.util.UUID;
import kr.ac.skku.scg.exhibition.exhibition.dto.request.AdminExhibitionCreateRequest;
import kr.ac.skku.scg.exhibition.exhibition.dto.response.ExhibitionResponse;
import kr.ac.skku.scg.exhibition.exhibition.service.AdminExhibitionService;
import kr.ac.skku.scg.exhibition.global.config.SecurityConfig;
import kr.ac.skku.scg.exhibition.global.config.WebConfig;
import kr.ac.skku.scg.exhibition.global.error.ApiExceptionHandler;
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

@WebMvcTest(controllers = AdminExhibitionController.class)
@Import({SecurityConfig.class, ApiExceptionHandler.class, WebConfig.class})
class AdminExhibitionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminExhibitionService adminExhibitionService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void create_requiresAuthentication() throws Exception {
        mockMvc.perform(post("/admin/exhibitions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Authentication is required"));
    }

    @Test
    void create_rejectsMalformedDomain() throws Exception {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.of(admin(userId)));

        mockMvc.perform(post("/admin/exhibitions")
                        .with(user(userId.toString()))
                        .requestAttr("auth.userId", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson().replace("graduation.example.com", "https://bad.example.com:443/path")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void create_returnsCreatedExhibition() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID exhibitionId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.of(admin(userId)));
        when(adminExhibitionService.create(any(), any())).thenReturn(response(exhibitionId));

        mockMvc.perform(post("/admin/exhibitions")
                        .with(user(userId.toString()))
                        .requestAttr("auth.userId", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/admin/exhibitions/" + exhibitionId))
                .andExpect(jsonPath("$.id").value(exhibitionId.toString()));
    }

    @Test
    void delete_returnsNoContent() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID exhibitionId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.of(admin(userId)));

        mockMvc.perform(delete("/admin/exhibitions/{exhibitionId}", exhibitionId)
                        .with(user(userId.toString()))
                        .requestAttr("auth.userId", userId))
                .andExpect(status().isNoContent());
    }

    private String requestJson() {
        return """
                {
                  "slug": "graduation-2026",
                  "defaultDomain": "graduation.example.com",
                  "name": "졸업 전시",
                  "description": "설명"
                }
                """;
    }

    private ExhibitionResponse response(UUID id) {
        return new ExhibitionResponse(
                id, "graduation-2026", "graduation.example.com", null, "졸업 전시", "설명",
                null, false, null, false, null, null, null, null, null);
    }

    private UserEntity admin(UUID id) {
        UserEntity user = new UserEntity(id, "kakao:test", "관리자", "admin@example.com", UserType.ADMIN);
        user.completeRegistration("관리자", "admin@example.com", null, null, null);
        return user;
    }
}
