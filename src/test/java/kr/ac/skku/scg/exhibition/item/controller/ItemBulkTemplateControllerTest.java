package kr.ac.skku.scg.exhibition.item.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;
import java.util.UUID;
import kr.ac.skku.scg.exhibition.global.config.SecurityConfig;
import kr.ac.skku.scg.exhibition.global.config.WebConfig;
import kr.ac.skku.scg.exhibition.global.error.ApiExceptionHandler;
import kr.ac.skku.scg.exhibition.item.dto.response.ItemBulkTemplateFile;
import kr.ac.skku.scg.exhibition.item.service.ItemBulkTemplateService;
import kr.ac.skku.scg.exhibition.user.domain.UserEntity;
import kr.ac.skku.scg.exhibition.user.domain.UserType;
import kr.ac.skku.scg.exhibition.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ItemBulkTemplateController.class)
@Import({SecurityConfig.class, ApiExceptionHandler.class, WebConfig.class})
class ItemBulkTemplateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ItemBulkTemplateService itemBulkTemplateService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void downloadTemplate() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID exhibitionId = UUID.randomUUID();
        UUID eventPeriodId = UUID.randomUUID();

        when(userRepository.findById(userId)).thenReturn(Optional.of(testAdminUser(userId)));
        when(itemBulkTemplateService.generateTemplate(any(), any())).thenReturn(
                new ItemBulkTemplateFile("item-bulk-template.xlsx", new byte[] {1, 2, 3}));

        mockMvc.perform(get("/admin/items/bulk/template")
                        .requestAttr("auth.userId", userId)
                        .param("exhibitionId", exhibitionId.toString())
                        .param("eventPeriodId", eventPeriodId.toString()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"item-bulk-template.xlsx\""));
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
