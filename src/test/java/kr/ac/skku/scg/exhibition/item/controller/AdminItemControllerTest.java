package kr.ac.skku.scg.exhibition.item.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AdminItemController.class)
@Import({SecurityConfig.class, ApiExceptionHandler.class, WebConfig.class})
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
        when(exhibitionDomainCacheService.findByDomain("example.com")).thenReturn(Optional.of(exhibition));
        ItemResponse response = new ItemResponse(itemId, exhibitionId, categoryId, null, "작품", null,
                null, null, null, null, null, null, false, 0, false);
        when(service.create(eq(exhibition), any(), eq(authenticatedUser))).thenReturn(response);
        when(service.update(eq(itemId), eq(exhibitionId), any(), eq(authenticatedUser))).thenReturn(response);
        String body = "{\"categoryId\":\"" + categoryId + "\",\"title\":\"작품\",\"awarded\":false}";

        mockMvc.perform(post("/admin/items").header("Host", "example.com").with(user(userId.toString()))
                        .requestAttr("auth.userId", userId).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mockMvc.perform(put("/admin/items/{id}", itemId).header("Host", "example.com").with(user(userId.toString()))
                        .requestAttr("auth.userId", userId).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/admin/items/{id}", itemId).header("Host", "example.com").with(user(userId.toString()))
                        .requestAttr("auth.userId", userId))
                .andExpect(status().isNoContent());

        verify(service).create(eq(exhibition), any(), eq(authenticatedUser));
        verify(service).update(eq(itemId), eq(exhibitionId), any(), eq(authenticatedUser));
        verify(service).delete(itemId, exhibitionId, authenticatedUser);
    }

    private UserEntity admin(UUID id) {
        UserEntity user = new UserEntity(id, "kakao:test", "관리자", "admin@example.com", UserType.ADMIN);
        user.completeRegistration("관리자", "admin@example.com", null, null, null);
        return user;
    }
}
