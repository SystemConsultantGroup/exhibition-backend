package kr.ac.skku.scg.exhibition.item.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.minio.MinioClient;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import kr.ac.skku.scg.exhibition.category.domain.CategoryEntity;
import kr.ac.skku.scg.exhibition.category.repository.CategoryRepository;
import kr.ac.skku.scg.exhibition.classification.repository.ItemClassificationMapRepository;
import kr.ac.skku.scg.exhibition.eventperiod.domain.EventPeriodEntity;
import kr.ac.skku.scg.exhibition.eventperiod.repository.EventPeriodRepository;
import kr.ac.skku.scg.exhibition.exhibition.domain.ExhibitionEntity;
import kr.ac.skku.scg.exhibition.global.auth.resolver.AuthenticatedUser;
import kr.ac.skku.scg.exhibition.global.auth.service.AdminAuthorizationService;
import kr.ac.skku.scg.exhibition.global.config.MinioProperties;
import kr.ac.skku.scg.exhibition.global.error.ForbiddenException;
import kr.ac.skku.scg.exhibition.global.error.NotFoundException;
import kr.ac.skku.scg.exhibition.item.domain.ItemEntity;
import kr.ac.skku.scg.exhibition.item.dto.request.AdminItemRequest;
import kr.ac.skku.scg.exhibition.item.dto.response.ItemResponse;
import kr.ac.skku.scg.exhibition.item.repository.ItemLikeRepository;
import kr.ac.skku.scg.exhibition.item.repository.ItemRepository;
import kr.ac.skku.scg.exhibition.media.repository.MediaAssetRepository;
import kr.ac.skku.scg.exhibition.user.domain.UserType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminItemServiceTest {
    @Mock ItemRepository items;
    @Mock CategoryRepository categories;
    @Mock EventPeriodRepository periods;
    @Mock ItemLikeRepository likes;
    @Mock ItemClassificationMapRepository mappings;
    @Mock MediaAssetRepository media;
    @Mock AdminAuthorizationService authorization;
    @Mock MinioClient minio;
    private AdminItemService service;

    @BeforeEach
    void setUp() {
        MinioProperties properties = new MinioProperties();
        properties.setBucket("exhibition");
        service = new AdminItemService(items, categories, periods, likes, mappings, media,
                authorization, minio, properties);
    }

    @Test
    void create_allowsAssignedAdminAndUsesScopedRelations() {
        UUID exhibitionId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        UUID periodId = UUID.randomUUID();
        ExhibitionEntity exhibition = new ExhibitionEntity(exhibitionId, "slug", "전시");
        CategoryEntity category = new CategoryEntity(categoryId, exhibition, "AI");
        EventPeriodEntity period = new EventPeriodEntity(periodId, exhibition, "2026", null, null);
        when(categories.findByIdAndExhibition_Id(categoryId, exhibitionId)).thenReturn(Optional.of(category));
        when(periods.findByIdAndExhibition_Id(periodId, exhibitionId)).thenReturn(Optional.of(period));
        when(items.save(any(ItemEntity.class))).thenAnswer(invocation -> {
            ItemEntity input = invocation.getArgument(0);
            ItemEntity saved = new ItemEntity(UUID.randomUUID(), exhibition, category, period,
                    input.getTitle(), input.getDescription());
            saved.updateMetadata(category, period, input.getTitle(), input.getDescription(),
                    input.getParticipantNames(), input.getParticipantEmails(), input.getAdvisorNames(), input.isAwarded());
            return saved;
        });

        ItemResponse response = service.create(exhibition, request(categoryId, periodId), user(UserType.SUB_ADMIN));

        assertThat(response.title()).isEqualTo("작품");
        verify(authorization).requireExhibitionAdmin(any(), org.mockito.ArgumentMatchers.eq(exhibitionId));
    }

    @Test
    void update_rejectsCrossTenantItemId() {
        UUID exhibitionId = UUID.randomUUID();
        when(items.findByIdAndExhibition_Id(any(), org.mockito.ArgumentMatchers.eq(exhibitionId)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(UUID.randomUUID(), exhibitionId,
                request(UUID.randomUUID(), null), user(UserType.ADMIN)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void delete_removesScopedDependencies() {
        UUID exhibitionId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        ExhibitionEntity exhibition = new ExhibitionEntity(exhibitionId, "slug", "전시");
        ItemEntity item = new ItemEntity(itemId, exhibition,
                new CategoryEntity(UUID.randomUUID(), exhibition, "AI"), null, "작품", null);
        when(items.findByIdAndExhibition_Id(itemId, exhibitionId)).thenReturn(Optional.of(item));
        when(media.findAllByItem_Id(itemId)).thenReturn(List.of());

        service.delete(itemId, exhibitionId, user(UserType.ADMIN));

        verify(likes).deleteAllByItem_Id(itemId);
        verify(mappings).deleteAllByItem_Id(itemId);
        verify(media).deleteAllByItem_Id(itemId);
        verify(items).delete(item);
    }

    @Test
    void create_stopsOnAuthorizationFailure() {
        ExhibitionEntity exhibition = new ExhibitionEntity(UUID.randomUUID(), "slug", "전시");
        AuthenticatedUser visitor = user(UserType.VISITOR);
        doThrow(new ForbiddenException("forbidden"))
                .when(authorization).requireExhibitionAdmin(visitor, exhibition.getId());

        assertThatThrownBy(() -> service.create(exhibition,
                request(UUID.randomUUID(), null), visitor)).isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(categories, periods, items);
    }

    private AdminItemRequest request(UUID categoryId, UUID periodId) {
        return new AdminItemRequest(categoryId, periodId, "작품", "설명", "학생", "a@example.com", "교수", false);
    }

    private AuthenticatedUser user(UserType role) {
        return new AuthenticatedUser(UUID.randomUUID(), "user", role, null, null, null, null, true);
    }
}
