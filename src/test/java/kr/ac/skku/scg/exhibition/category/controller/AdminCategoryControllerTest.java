package kr.ac.skku.scg.exhibition.category.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import kr.ac.skku.scg.exhibition.category.dto.response.CategoryResponse;
import kr.ac.skku.scg.exhibition.category.service.CategoryService;
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
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AdminCategoryController.class)
@Import({SecurityConfig.class, ApiExceptionHandler.class, WebConfig.class})
class AdminCategoryControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private CategoryService service;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private ExhibitionDomainCacheService exhibitionDomainCacheService;

    @Test
    void delegatesUpdateAndReorderWithResolvedTenantAndUser() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID exhibitionId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        ExhibitionEntity exhibition = new ExhibitionEntity(exhibitionId, "slug", "전시");
        UserEntity admin = admin(userId);
        AuthenticatedUser authenticatedUser = AuthenticatedUser.from(admin);
        when(userRepository.findById(userId)).thenReturn(Optional.of(admin));
        when(exhibitionDomainCacheService.findByDomain("example.com")).thenReturn(Optional.of(exhibition));
        CategoryResponse response = new CategoryResponse(categoryId, exhibitionId, "new-name", "새 이름");
        when(service.update(eq(categoryId), eq(exhibitionId), any(), eq(authenticatedUser))).thenReturn(response);
        when(service.reorder(eq(exhibitionId), any(), eq(authenticatedUser))).thenReturn(List.of(response));

        mockMvc.perform(put("/admin/categories/{id}", categoryId).header("Host", "example.com")
                        .with(user(userId.toString())).requestAttr("auth.userId", userId)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"새 이름\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(put("/admin/categories/order").header("Host", "example.com")
                        .with(user(userId.toString())).requestAttr("auth.userId", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoryIds\":[\"" + categoryId + "\"]}"))
                .andExpect(status().isOk());

        verify(service).update(eq(categoryId), eq(exhibitionId), any(), eq(authenticatedUser));
        verify(service).reorder(eq(exhibitionId), any(), eq(authenticatedUser));
    }

    private UserEntity admin(UUID id) {
        UserEntity user = new UserEntity(id, "kakao:test", "관리자", "admin@example.com", UserType.ADMIN);
        user.completeRegistration("관리자", "admin@example.com", null, null, null);
        return user;
    }
}
