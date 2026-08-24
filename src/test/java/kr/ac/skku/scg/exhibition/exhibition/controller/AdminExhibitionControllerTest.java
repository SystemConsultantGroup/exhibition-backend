package kr.ac.skku.scg.exhibition.exhibition.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestPartFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.partWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.requestParts;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import kr.ac.skku.scg.exhibition.exhibition.dto.request.AdminExhibitionCreateRequest;
import kr.ac.skku.scg.exhibition.exhibition.dto.response.ExhibitionResponse;
import kr.ac.skku.scg.exhibition.exhibition.service.AdminExhibitionService;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AdminExhibitionController.class)
@Import({SecurityConfig.class, ApiExceptionHandler.class, WebConfig.class})
@AutoConfigureRestDocs
class AdminExhibitionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminExhibitionService adminExhibitionService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private ExhibitionDomainCacheService exhibitionDomainCacheService;

    @Test
    void list_returnsAccessibleExhibitions() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID exhibitionId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.of(admin(userId)));
        when(adminExhibitionService.listAccessible(any())).thenReturn(List.of(response(exhibitionId)));

        mockMvc.perform(get("/admin/exhibitions")
                        .header("Authorization", "Bearer access-token")
                        .with(user(userId.toString()))
                        .requestAttr("auth.userId", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(exhibitionId.toString()))
                .andExpect(jsonPath("$.total").value(1))
                .andDo(document("admin-exhibitions-list",
                        authorizationHeader(),
                        responseFields(
                                fieldWithPath("items").description("관리 가능한 전시 목록"),
                                fieldWithPath("items[].id").description("전시 ID"),
                                fieldWithPath("items[].slug").description("전시 slug"),
                                fieldWithPath("items[].defaultDomain").description("기본 도메인").optional(),
                                fieldWithPath("items[].customDomain").description("커스텀 도메인").optional(),
                                fieldWithPath("items[].name").description("전시 이름"),
                                fieldWithPath("items[].description").description("전시 설명").optional(),
                                fieldWithPath("items[].logoMediaId").description("로고 미디어 ID").optional(),
                                fieldWithPath("items[].bannerEnabled").description("배너 활성화 여부"),
                                fieldWithPath("items[].bannerMediaId").description("배너 미디어 ID").optional(),
                                fieldWithPath("items[].popupEnabled").description("팝업 활성화 여부"),
                                fieldWithPath("items[].popupImageMediaId").description("팝업 이미지 미디어 ID").optional(),
                                fieldWithPath("items[].popupUrl").description("팝업 이동 URL").optional(),
                                fieldWithPath("items[].introTitle").description("인트로 제목").optional(),
                                fieldWithPath("items[].introDescription").description("인트로 설명").optional(),
                                fieldWithPath("items[].introVideoMediaId").description("인트로 영상 미디어 ID").optional(),
                                fieldWithPath("page").description("페이지 번호"),
                                fieldWithPath("pageSize").description("페이지 크기"),
                                fieldWithPath("total").description("전체 개수")
                        )));
    }

    @Test
    void list_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/admin/exhibitions"))
                .andExpect(status().isUnauthorized());
    }

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

        MockMultipartFile request = new MockMultipartFile(
                "request", "", MediaType.APPLICATION_JSON_VALUE,
                requestJson().replace("graduation.example.com", "https://bad.example.com:443/path").getBytes());

        mockMvc.perform(multipart("/admin/exhibitions")
                        .file(request)
                        .with(user(userId.toString()))
                        .requestAttr("auth.userId", userId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void create_returnsCreatedExhibition() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID exhibitionId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.of(admin(userId)));
        when(adminExhibitionService.create(any(), any(), any(), any(), any(), any()))
                .thenReturn(response(exhibitionId));
        MockMultipartFile request = new MockMultipartFile(
                "request", "", MediaType.APPLICATION_JSON_VALUE, requestJson().getBytes());
        MockMultipartFile logo = new MockMultipartFile(
                "logoFile", "logo.png", MediaType.IMAGE_PNG_VALUE,
                new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a});

        mockMvc.perform(multipart("/admin/exhibitions")
                        .file(request)
                        .file(logo)
                        .header("Authorization", "Bearer access-token")
                        .with(user(userId.toString()))
                        .requestAttr("auth.userId", userId))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/admin/exhibitions/" + exhibitionId))
                .andExpect(jsonPath("$.id").value(exhibitionId.toString()))
                .andDo(document("admin-exhibitions-create",
                        authorizationHeader(),
                        requestParts(
                                partWithName("request").description("전시 생성 정보 JSON"),
                                partWithName("logoFile").description("로고 이미지 파일 (nullable: PNG/JPEG/GIF/WebP)").optional(),
                                partWithName("bannerFile").description("배너 이미지 파일 (nullable: PNG/JPEG/GIF/WebP)").optional(),
                                partWithName("popupImageFile").description("팝업 이미지 파일 (nullable: PNG/JPEG/GIF/WebP)").optional(),
                                partWithName("introVideoFile").description("인트로 영상 파일 (nullable: MP4/WebM/MOV)").optional()
                        ),
                        requestPartFields("request",
                                fieldWithPath("slug").description("전시 slug (영문 소문자, 숫자, 하이픈)"),
                                fieldWithPath("defaultDomain").description("기본 도메인 호스트"),
                                fieldWithPath("customDomain").description("커스텀 도메인 호스트 (nullable)").optional(),
                                fieldWithPath("name").description("전시 이름"),
                                fieldWithPath("description").description("전시 설명 (nullable)").optional(),
                                fieldWithPath("bannerEnabled").description("배너 활성화 여부"),
                                fieldWithPath("popupEnabled").description("팝업 활성화 여부"),
                                fieldWithPath("popupUrl").description("팝업 이동 URL (nullable)").optional(),
                                fieldWithPath("introTitle").description("인트로 제목 (nullable, 최대 200자)").optional(),
                                fieldWithPath("introDescription").description("인트로 설명 (nullable)").optional()
                        ),
                        responseFields(exhibitionResponseFields())));

        verify(adminExhibitionService).create(any(), any(), any(), any(), any(), any());
    }

    @Test
    void update_returnsUpdatedExhibition() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID exhibitionId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.of(admin(userId)));
        when(adminExhibitionService.update(any(), any(), any())).thenReturn(response(exhibitionId));

        mockMvc.perform(put("/admin/exhibitions/{exhibitionId}", exhibitionId)
                        .header("Authorization", "Bearer access-token")
                        .with(user(userId.toString()))
                        .requestAttr("auth.userId", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateRequestJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(exhibitionId.toString()))
                .andDo(document("admin-exhibitions-update",
                        authorizationHeader(),
                        pathParameters(parameterWithName("exhibitionId").description("수정할 전시 ID")),
                        exhibitionRequestFields(),
                        responseFields(exhibitionResponseFields())));
    }

    @Test
    void delete_returnsNoContent() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID exhibitionId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.of(admin(userId)));

        mockMvc.perform(delete("/admin/exhibitions/{exhibitionId}", exhibitionId)
                        .header("Authorization", "Bearer access-token")
                        .with(user(userId.toString()))
                        .requestAttr("auth.userId", userId))
                .andExpect(status().isNoContent())
                .andDo(document("admin-exhibitions-delete",
                        authorizationHeader(),
                        pathParameters(parameterWithName("exhibitionId").description("삭제할 전시 ID"))));
    }

    private String requestJson() {
        return """
                {
                  "slug": "graduation-2026",
                  "defaultDomain": "graduation.example.com",
                  "customDomain": null,
                  "name": "졸업 전시",
                  "description": null,
                  "bannerEnabled": true,
                  "popupEnabled": true,
                  "popupUrl": null,
                  "introTitle": null,
                  "introDescription": null
                }
                """;
    }

    private String updateRequestJson() {
        return """
                {
                  "defaultDomain": "graduation.example.com",
                  "customDomain": "custom.example.com",
                  "name": "졸업 전시",
                  "description": "설명",
                  "logoMediaId": null,
                  "bannerEnabled": true,
                  "bannerMediaId": null,
                  "popupEnabled": true,
                  "popupImageMediaId": null,
                  "popupUrl": "https://example.com/notice",
                  "introTitle": "인트로",
                  "introDescription": "전시 소개",
                  "introVideoMediaId": null
                }
                """;
    }

    private org.springframework.restdocs.headers.RequestHeadersSnippet authorizationHeader() {
        return requestHeaders(headerWithName("Authorization").description("Bearer access token"));
    }

    private org.springframework.restdocs.payload.RequestFieldsSnippet exhibitionRequestFields() {
        return requestFields(
                fieldWithPath("defaultDomain").description("기본 도메인 호스트"),
                fieldWithPath("customDomain").description("커스텀 도메인 호스트 (nullable)").optional(),
                fieldWithPath("name").description("전시 이름"),
                fieldWithPath("description").description("전시 설명 (nullable)").optional(),
                fieldWithPath("logoMediaId").description("로고 미디어 ID (nullable)").optional(),
                fieldWithPath("bannerEnabled").description("배너 활성화 여부").optional(),
                fieldWithPath("bannerMediaId").description("배너 미디어 ID (nullable)").optional(),
                fieldWithPath("popupEnabled").description("팝업 활성화 여부").optional(),
                fieldWithPath("popupImageMediaId").description("팝업 이미지 미디어 ID (nullable)").optional(),
                fieldWithPath("popupUrl").description("팝업 이동 URL (nullable)").optional(),
                fieldWithPath("introTitle").description("인트로 제목 (nullable, 최대 200자)").optional(),
                fieldWithPath("introDescription").description("인트로 설명 (nullable)").optional(),
                fieldWithPath("introVideoMediaId").description("인트로 영상 미디어 ID (nullable)").optional()
        );
    }

    private org.springframework.restdocs.payload.FieldDescriptor[] exhibitionResponseFields() {
        return new org.springframework.restdocs.payload.FieldDescriptor[] {
                fieldWithPath("id").description("전시 ID"),
                fieldWithPath("slug").description("전시 slug"),
                fieldWithPath("defaultDomain").description("기본 도메인 (nullable)").optional(),
                fieldWithPath("customDomain").description("커스텀 도메인 (nullable)").optional(),
                fieldWithPath("name").description("전시 이름"),
                fieldWithPath("description").description("전시 설명 (nullable)").optional(),
                fieldWithPath("logoMediaId").description("로고 미디어 ID (nullable)").optional(),
                fieldWithPath("bannerEnabled").description("배너 활성화 여부"),
                fieldWithPath("bannerMediaId").description("배너 미디어 ID (nullable)").optional(),
                fieldWithPath("popupEnabled").description("팝업 활성화 여부"),
                fieldWithPath("popupImageMediaId").description("팝업 이미지 미디어 ID (nullable)").optional(),
                fieldWithPath("popupUrl").description("팝업 이동 URL (nullable)").optional(),
                fieldWithPath("introTitle").description("인트로 제목 (nullable)").optional(),
                fieldWithPath("introDescription").description("인트로 설명 (nullable)").optional(),
                fieldWithPath("introVideoMediaId").description("인트로 영상 미디어 ID (nullable)").optional()
        };
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
