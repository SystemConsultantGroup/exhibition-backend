package kr.ac.skku.scg.exhibition.item.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;
import java.util.UUID;
import kr.ac.skku.scg.exhibition.global.config.SecurityConfig;
import kr.ac.skku.scg.exhibition.global.config.WebConfig;
import kr.ac.skku.scg.exhibition.global.error.ApiExceptionHandler;
import kr.ac.skku.scg.exhibition.item.dto.response.ItemBulkUploadResponse;
import kr.ac.skku.scg.exhibition.item.service.ItemBulkUploadService;
import kr.ac.skku.scg.exhibition.user.domain.UserEntity;
import kr.ac.skku.scg.exhibition.user.domain.UserType;
import kr.ac.skku.scg.exhibition.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ItemBulkUploadController.class)
@Import({SecurityConfig.class, ApiExceptionHandler.class, WebConfig.class})
class ItemBulkUploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ItemBulkUploadService itemBulkUploadService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void upload() throws Exception {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.of(testAdminUser(userId)));
        when(itemBulkUploadService.upload(any(), any())).thenReturn(new ItemBulkUploadResponse(2, 3, 4));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "items.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[] {1, 2, 3}
        );

        mockMvc.perform(multipart("/admin/items/bulk/upload")
                        .file(file)
                        .requestAttr("auth.userId", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.createdItems").value(2))
                .andExpect(jsonPath("$.createdMediaAssets").value(3))
                .andExpect(jsonPath("$.createdClassificationMappings").value(4));
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
