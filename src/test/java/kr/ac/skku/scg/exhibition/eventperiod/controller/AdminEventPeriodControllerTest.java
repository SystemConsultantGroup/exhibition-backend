package kr.ac.skku.scg.exhibition.eventperiod.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import kr.ac.skku.scg.exhibition.eventperiod.dto.response.EventPeriodResponse;
import kr.ac.skku.scg.exhibition.eventperiod.service.EventPeriodService;
import kr.ac.skku.scg.exhibition.exhibition.domain.ExhibitionEntity;
import kr.ac.skku.scg.exhibition.global.auth.resolver.AuthenticatedUser;
import kr.ac.skku.scg.exhibition.global.config.SecurityConfig;
import kr.ac.skku.scg.exhibition.global.config.WebConfig;
import kr.ac.skku.scg.exhibition.global.error.ApiExceptionHandler;
import kr.ac.skku.scg.exhibition.global.tenant.ExhibitionDomainCacheService;
import kr.ac.skku.scg.exhibition.user.domain.UserEntity;
import kr.ac.skku.scg.exhibition.user.domain.UserType;
import kr.ac.skku.scg.exhibition.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AdminEventPeriodController.class)
@Import({SecurityConfig.class, ApiExceptionHandler.class, WebConfig.class})
@AutoConfigureRestDocs
class AdminEventPeriodControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private EventPeriodService service;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private ExhibitionDomainCacheService exhibitionDomainCacheService;

    @Test
    void delegatesCreateUpdateAndDeleteWithResolvedTenantAndUser() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID exhibitionId = UUID.randomUUID();
        UUID periodId = UUID.randomUUID();
        ExhibitionEntity exhibition = new ExhibitionEntity(exhibitionId, "slug", "전시");
        UserEntity admin = admin(userId);
        AuthenticatedUser authenticatedUser = AuthenticatedUser.from(admin);
        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        Instant end = Instant.parse("2026-12-31T00:00:00Z");
        EventPeriodResponse response = new EventPeriodResponse(periodId, exhibitionId, "2026", start, end);
        when(userRepository.findById(userId)).thenReturn(Optional.of(admin));
        when(exhibitionDomainCacheService.findByDomain("example.com")).thenReturn(Optional.of(exhibition));
        when(service.create(eq(exhibition), any(), eq(authenticatedUser))).thenReturn(response);
        when(service.update(eq(periodId), eq(exhibitionId), any(), eq(authenticatedUser))).thenReturn(response);
        String body = "{\"name\":\"2026\",\"startTime\":\"" + start + "\",\"endTime\":\"" + end + "\"}";

        mockMvc.perform(post("/admin/event-periods").header("Host", "example.com").with(user(userId.toString()))
                        .requestAttr("auth.userId", userId).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andDo(document("admin-event-periods-create", eventPeriodRequestFields()));
        mockMvc.perform(put("/admin/event-periods/{id}", periodId).header("Host", "example.com")
                        .with(user(userId.toString())).requestAttr("auth.userId", userId)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andDo(document("admin-event-periods-update",
                        pathParameters(parameterWithName("id").description("수정할 발표회차 ID")),
                        eventPeriodRequestFields()));
        mockMvc.perform(delete("/admin/event-periods/{id}", periodId).header("Host", "example.com")
                        .with(user(userId.toString())).requestAttr("auth.userId", userId))
                .andExpect(status().isNoContent())
                .andDo(document("admin-event-periods-delete",
                        pathParameters(parameterWithName("id").description("삭제할 발표회차 ID"))));

        verify(service).create(eq(exhibition), any(), eq(authenticatedUser));
        verify(service).update(eq(periodId), eq(exhibitionId), any(), eq(authenticatedUser));
        verify(service).delete(periodId, exhibitionId, authenticatedUser);
    }

    private org.springframework.restdocs.payload.RequestFieldsSnippet eventPeriodRequestFields() {
        return requestFields(
                fieldWithPath("name").description("발표회차 이름 (최대 50자)"),
                fieldWithPath("startTime").description("시작 시각 (ISO-8601)"),
                fieldWithPath("endTime").description("종료 시각 (ISO-8601)")
        );
    }

    private UserEntity admin(UUID id) {
        UserEntity user = new UserEntity(id, "kakao:test", "관리자", "admin@example.com", UserType.ADMIN);
        user.completeRegistration("관리자", "admin@example.com", null, null, null);
        return user;
    }
}
