package kr.ac.skku.scg.exhibition.category.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import kr.ac.skku.scg.exhibition.category.domain.CategoryEntity;
import kr.ac.skku.scg.exhibition.category.dto.request.AdminCategoryOrderRequest;
import kr.ac.skku.scg.exhibition.category.dto.request.AdminCategoryUpdateRequest;
import kr.ac.skku.scg.exhibition.category.repository.CategoryRepository;
import kr.ac.skku.scg.exhibition.exhibition.domain.ExhibitionEntity;
import kr.ac.skku.scg.exhibition.global.auth.resolver.AuthenticatedUser;
import kr.ac.skku.scg.exhibition.global.auth.service.AdminAuthorizationService;
import kr.ac.skku.scg.exhibition.global.error.ConflictException;
import kr.ac.skku.scg.exhibition.user.domain.UserType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {
    @Mock CategoryRepository repository;
    @Mock AdminAuthorizationService authorization;
    private CategoryService service;

    @BeforeEach
    void setUp() { service = new CategoryService(repository, authorization); }

    @Test
    void update_rejectsNameCollision() {
        UUID exhibitionId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        ExhibitionEntity exhibition = new ExhibitionEntity(exhibitionId, "slug", "전시");
        CategoryEntity category = new CategoryEntity(categoryId, exhibition, "기존");
        when(repository.findByIdAndExhibition_Id(categoryId, exhibitionId)).thenReturn(Optional.of(category));
        when(repository.existsByExhibition_IdAndNameAndIdNot(exhibitionId, "중복", categoryId)).thenReturn(true);

        assertThatThrownBy(() -> service.update(categoryId, exhibitionId,
                new AdminCategoryUpdateRequest("중복"), user(UserType.SUB_ADMIN)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void reorder_rewritesCreatedAtInRequestedOrder() {
        UUID exhibitionId = UUID.randomUUID();
        ExhibitionEntity exhibition = new ExhibitionEntity(exhibitionId, "slug", "전시");
        CategoryEntity first = new CategoryEntity(UUID.randomUUID(), exhibition, "첫째");
        CategoryEntity second = new CategoryEntity(UUID.randomUUID(), exhibition, "둘째");
        AuthenticatedUser subAdmin = user(UserType.SUB_ADMIN);
        when(repository.findAllByExhibition_Id(exhibitionId)).thenReturn(List.of(first, second));

        service.reorder(exhibitionId, new AdminCategoryOrderRequest(List.of(second.getId(), first.getId())), subAdmin);

        verify(authorization).requireExhibitionAdmin(subAdmin, exhibitionId);
        assertThat(second.getCreatedAt()).isBefore(first.getCreatedAt());
    }

    @Test
    void reorder_rejectsIncompleteIds() {
        UUID exhibitionId = UUID.randomUUID();
        ExhibitionEntity exhibition = new ExhibitionEntity(exhibitionId, "slug", "전시");
        CategoryEntity category = new CategoryEntity(UUID.randomUUID(), exhibition, "카테고리");
        when(repository.findAllByExhibition_Id(exhibitionId)).thenReturn(List.of(category));

        assertThatThrownBy(() -> service.reorder(exhibitionId,
                new AdminCategoryOrderRequest(List.of(UUID.randomUUID())), user(UserType.ADMIN)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** 생성도 전시 권한과 이름/슬러그 중복을 확인한다. */
    @Test
    void create_validatesAndNormalizesName() {
        ExhibitionEntity exhibition = new ExhibitionEntity(UUID.randomUUID(), "slug", "전시");
        when(repository.saveAndFlush(org.mockito.ArgumentMatchers.any())).thenAnswer(call -> call.getArgument(0));
        var result = service.create(exhibition, new AdminCategoryUpdateRequest(" AI "), user(UserType.ADMIN));
        assertThat(result.name()).isEqualTo("AI");
        verify(authorization).requireExhibitionAdmin(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(exhibition.getId()));
    }

    /** 참조 무결성 위반은 삭제 실패 충돌로 변환한다. */
    @Test
    void delete_rejectsReferencedCategory() {
        ExhibitionEntity exhibition = new ExhibitionEntity(UUID.randomUUID(), "slug", "전시");
        CategoryEntity category = new CategoryEntity(UUID.randomUUID(), exhibition, "AI");
        when(repository.findByIdAndExhibition_Id(category.getId(), exhibition.getId())).thenReturn(Optional.of(category));
        org.mockito.Mockito.doThrow(new org.springframework.dao.DataIntegrityViolationException("referenced"))
                .when(repository).flush();
        assertThatThrownBy(() -> service.delete(category.getId(), exhibition.getId(), user(UserType.ADMIN)))
                .isInstanceOf(ConflictException.class);
    }

    private AuthenticatedUser user(UserType type) {
        return new AuthenticatedUser(UUID.randomUUID(), "user", type, null, null, null, null, true);
    }
}
