package kr.ac.skku.scg.exhibition.item.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestPartFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.partWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.requestParts;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;
import java.util.UUID;
import kr.ac.skku.scg.exhibition.exhibition.domain.ExhibitionEntity;
import kr.ac.skku.scg.exhibition.global.auth.resolver.AuthenticatedUser;
import kr.ac.skku.scg.exhibition.global.config.SecurityConfig;
import kr.ac.skku.scg.exhibition.global.config.WebConfig;
import kr.ac.skku.scg.exhibition.global.error.ApiExceptionHandler;
import kr.ac.skku.scg.exhibition.global.tenant.ExhibitionDomainCacheService;
import kr.ac.skku.scg.exhibition.item.dto.response.ItemResponse;
import kr.ac.skku.scg.exhibition.item.service.AdminItemService;
import kr.ac.skku.scg.exhibition.user.domain.UserEntity;
import kr.ac.skku.scg.exhibition.user.domain.UserType;
import kr.ac.skku.scg.exhibition.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AdminItemController.class)
@Import({SecurityConfig.class, ApiExceptionHandler.class, WebConfig.class})
@AutoConfigureRestDocs
class AdminItemControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private AdminItemService service;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private ExhibitionDomainCacheService exhibitionDomainCacheService;

    @Test
    void delegatesCreateUpdateAndDeleteWithResolvedTenantAndUser() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID exhibitionId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        ExhibitionEntity exhibition = new ExhibitionEntity(exhibitionId, "slug", "전시");
        UserEntity admin = admin(userId);
        AuthenticatedUser authenticatedUser = AuthenticatedUser.from(admin);
        when(userRepository.findById(userId)).thenReturn(Optional.of(admin));
        when(exhibitionDomainCacheService.findById(exhibitionId)).thenReturn(Optional.of(exhibition));
        ItemResponse response = new ItemResponse(itemId, exhibitionId, categoryId, null, "작품", null,
                null, null, null, null, null, null, false, 0, false);
        when(service.create(eq(exhibition), any(), any(), any(), any(), eq(authenticatedUser))).thenReturn(response);
        when(service.update(eq(itemId), eq(exhibitionId), any(), eq(authenticatedUser))).thenReturn(response);
        String body = "{\"categoryId\":\"" + categoryId
                + "\",\"eventPeriodId\":null,\"title\":\"작품\",\"description\":\"설명\""
                + ",\"participantNames\":\"홍길동\",\"participantEmails\":\"user@example.com\""
                + ",\"advisorNames\":\"교수님\",\"awarded\":false}";

        MockMultipartFile request = new MockMultipartFile(
                "request", "", MediaType.APPLICATION_JSON_VALUE, body.getBytes());
        MockMultipartFile thumbnail = new MockMultipartFile(
                "thumbnailFile", "thumb.jpg", MediaType.IMAGE_JPEG_VALUE,
                new byte[] {(byte) 0xff, (byte) 0xd8, (byte) 0xff});
        mockMvc.perform(multipart("/admin/items").file(request).file(thumbnail)
                        .header("X-Exhibition-Id", exhibitionId).with(user(userId.toString()))
                        .requestAttr("auth.userId", userId))
                .andExpect(status().isCreated())
                .andDo(document("admin-items-create",
                        requestParts(
                                partWithName("request").description("항목 생성 정보 JSON"),
                                partWithName("thumbnailFile").description("썸네일 이미지 (nullable)").optional(),
                                partWithName("posterFile").description("포스터 이미지 (nullable)").optional(),
                                partWithName("presentationVideoFile").description("발표 영상 (nullable)").optional()
                        ),
                        requestPartFields("request",
                                fieldWithPath("categoryId").description("카테고리 ID"),
                                fieldWithPath("eventPeriodId").description("발표회차 ID (미디어 업로드 시 필수)"),
                                fieldWithPath("title").description("항목 제목 (최대 200자)"),
                                fieldWithPath("description").description("항목 설명 (nullable)").optional(),
                                fieldWithPath("participantNames").description("참여자 이름 (nullable)").optional(),
                                fieldWithPath("participantEmails").description("참여자 이메일 (nullable)").optional(),
                                fieldWithPath("advisorNames").description("지도교수 이름 (nullable)").optional(),
                                fieldWithPath("awarded").description("수상 여부")
                        )));
        mockMvc.perform(put("/admin/items/{id}", itemId).header("X-Exhibition-Id", exhibitionId).with(user(userId.toString()))
                        .requestAttr("auth.userId", userId).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andDo(document("admin-items-update",
                        pathParameters(parameterWithName("id").description("수정할 항목 ID")), itemRequestFields()));
        mockMvc.perform(delete("/admin/items/{id}", itemId).header("X-Exhibition-Id", exhibitionId).with(user(userId.toString()))
                        .requestAttr("auth.userId", userId))
                .andExpect(status().isNoContent())
                .andDo(document("admin-items-delete",
                        pathParameters(parameterWithName("id").description("삭제할 항목 ID"))));

        verify(service).create(eq(exhibition), any(), any(), any(), any(), eq(authenticatedUser));
        verify(service).update(eq(itemId), eq(exhibitionId), any(), eq(authenticatedUser));
        verify(service).delete(itemId, exhibitionId, authenticatedUser);
    }

    private org.springframework.restdocs.payload.RequestFieldsSnippet itemRequestFields() {
        return requestFields(
                fieldWithPath("categoryId").description("카테고리 ID"),
                fieldWithPath("eventPeriodId").description("발표회차 ID").optional(),
                fieldWithPath("title").description("항목 제목 (최대 200자)"),
                fieldWithPath("description").description("항목 설명").optional(),
                fieldWithPath("participantNames").description("참여자 이름").optional(),
                fieldWithPath("participantEmails").description("참여자 이메일").optional(),
                fieldWithPath("advisorNames").description("지도교수 이름").optional(),
                fieldWithPath("awarded").description("수상 여부").optional()
        );
    }

    private UserEntity admin(UUID id) {
        UserEntity user = new UserEntity(id, "kakao:test", "관리자", "admin@example.com", UserType.ADMIN);
        user.completeRegistration("관리자", "admin@example.com", null, null, null);
        return user;
    }
}
