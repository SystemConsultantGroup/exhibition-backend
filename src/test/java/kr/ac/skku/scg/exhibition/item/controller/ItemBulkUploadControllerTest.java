package kr.ac.skku.scg.exhibition.item.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.request.RequestDocumentation.partWithName;
import static org.springframework.restdocs.request.RequestDocumentation.requestParts;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;
import java.util.UUID;
import kr.ac.skku.scg.exhibition.exhibition.domain.ExhibitionEntity;
import kr.ac.skku.scg.exhibition.global.auth.resolver.AuthenticatedUser;
import kr.ac.skku.scg.exhibition.global.config.SecurityConfig;
import kr.ac.skku.scg.exhibition.global.config.WebConfig;
import kr.ac.skku.scg.exhibition.global.error.ApiExceptionHandler;
import kr.ac.skku.scg.exhibition.global.tenant.ExhibitionDomainCacheService;
import kr.ac.skku.scg.exhibition.item.dto.response.ItemBulkUploadResponse;
import kr.ac.skku.scg.exhibition.item.service.ItemBulkUploadService;
import kr.ac.skku.scg.exhibition.user.domain.UserEntity;
import kr.ac.skku.scg.exhibition.user.domain.UserType;
import kr.ac.skku.scg.exhibition.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ItemBulkUploadController.class)
@Import({SecurityConfig.class, ApiExceptionHandler.class, WebConfig.class})
@AutoConfigureRestDocs
class ItemBulkUploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ItemBulkUploadService itemBulkUploadService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private ExhibitionDomainCacheService exhibitionDomainCacheService;

    @Test
    void upload_requiresAuthentication() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "items.xlsx", "application/octet-stream", new byte[] {1}
        );

        mockMvc.perform(multipart("/admin/items/bulk/upload").file(file))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void upload() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID exhibitionId = UUID.randomUUID();
        UserEntity admin = testAdminUser(userId);
        AuthenticatedUser authenticatedAdmin = AuthenticatedUser.from(admin);
        when(userRepository.findById(userId)).thenReturn(Optional.of(admin));
        when(exhibitionDomainCacheService.findById(exhibitionId)).thenReturn(Optional.of(
                new ExhibitionEntity(exhibitionId, "slug", "전시")));
        when(itemBulkUploadService.upload(any(), anyList(), eq(exhibitionId), eq(authenticatedAdmin)))
                .thenReturn(new ItemBulkUploadResponse(2, 3, 4));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "items.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[] {1, 2, 3}
        );

        MockMultipartFile mediaFile = new MockMultipartFile(
                "mediaFiles", "thumb.jpg", "image/jpeg", new byte[] {4, 5, 6}
        );

        mockMvc.perform(multipart("/admin/items/bulk/upload")
                        .file(file)
                        .file(mediaFile)
                        .header("X-Exhibition-Id", exhibitionId)
                        .requestAttr("auth.userId", userId)
                        .with(user(userId.toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.createdItems").value(2))
                .andExpect(jsonPath("$.createdMediaAssets").value(3))
                .andExpect(jsonPath("$.createdClassificationMappings").value(4))
                .andDo(document("admin-items-bulk-upload",
                        requestParts(
                                partWithName("file").description("항목 정보가 담긴 Excel 파일"),
                                partWithName("mediaFiles").description("Excel에서 참조하는 미디어 파일 목록").optional()
                        )));

        verify(itemBulkUploadService).upload(any(), anyList(), eq(exhibitionId), eq(authenticatedAdmin));
    }

    private UserEntity testAdminUser(UUID userId) {
        UserEntity user = new UserEntity(
                userId,
                "kakao:test",
                "관리자",
                "admin@example.com",
                null,
                null,
                null,
                UserType.ADMIN
        );
        user.completeRegistration("관리자", "admin@example.com", null, null, null);
        return user;
    }
}
