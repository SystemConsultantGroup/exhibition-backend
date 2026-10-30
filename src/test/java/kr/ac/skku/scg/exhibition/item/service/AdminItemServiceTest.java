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
import kr.ac.skku.scg.exhibition.media.service.MediaService;
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
    @Mock MediaService mediaService;
    @Mock kr.ac.skku.scg.exhibition.classification.repository.ItemClassificationRepository classifications;
    private AdminItemService service;

    @BeforeEach
    void setUp() {
        MinioProperties properties = new MinioProperties();
        properties.setBucket("exhibition");
        service = new AdminItemService(items, categories, periods, likes, mappings, media,
                authorization, minio, properties, mediaService, classifications);
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

        ItemResponse response = service.create(
                exhibition, request(categoryId, periodId), null, null, null, user(UserType.SUB_ADMIN));

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
                request(UUID.randomUUID(), null), null, null, null, visitor)).isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(categories, periods, items);
    }

    /** 파일이 없는 수정은 보존하고 제거 플래그는 지정한 슬롯만 해제한다. */
    @Test
    void update_preservesMediaAndCanExplicitlyRemoveIt() {
        ExhibitionEntity exhibition = new ExhibitionEntity(UUID.randomUUID(), "slug", "전시");
        CategoryEntity category = new CategoryEntity(UUID.randomUUID(), exhibition, "AI");
        ItemEntity item = new ItemEntity(UUID.randomUUID(), exhibition, category, null, "기존", null);
        var thumbnail = new kr.ac.skku.scg.exhibition.media.domain.MediaAssetEntity(
                UUID.randomUUID(), item, exhibition, "old.png", "image/png", 12);
        item.updateMedia(thumbnail, thumbnail, null);
        when(items.findByIdAndExhibition_Id(item.getId(), exhibition.getId())).thenReturn(Optional.of(item));
        when(categories.findByIdAndExhibition_Id(category.getId(), exhibition.getId())).thenReturn(Optional.of(category));
        service.update(item.getId(), exhibition.getId(), request(category.getId(), null), user(UserType.ADMIN));
        assertThat(item.getThumbnailMedia()).isSameAs(thumbnail);
        var remove = new AdminItemRequest(category.getId(), null, "수정", null, null, null, null, false,
                List.of(), true, false, false);
        service.update(item.getId(), exhibition.getId(), remove, user(UserType.ADMIN));
        assertThat(item.getThumbnailMedia()).isNull();
        assertThat(item.getPosterMedia()).isSameAs(thumbnail);
        verify(mappings).deleteAllByItem_Id(item.getId());
        verifyNoInteractions(mediaService);
    }

    /** multipart 교체는 미디어 업로드를 실행하고 다른 슬롯은 유지한다. */
    @Test
    void update_replacesOnlyProvidedMedia() {
        ExhibitionEntity exhibition = new ExhibitionEntity(UUID.randomUUID(), "slug", "전시");
        CategoryEntity category = new CategoryEntity(UUID.randomUUID(), exhibition, "AI");
        ItemEntity item = new ItemEntity(UUID.randomUUID(), exhibition, category, null, "기존", null);
        var file = new org.springframework.mock.web.MockMultipartFile("thumbnailFile", "new.png", "image/png", new byte[]{1});
        var replacement = new kr.ac.skku.scg.exhibition.media.domain.MediaAssetEntity(
                UUID.randomUUID(), item, exhibition, "new.png", "image/png", 1);
        when(items.findByIdAndExhibition_Id(item.getId(), exhibition.getId())).thenReturn(Optional.of(item));
        when(categories.findByIdAndExhibition_Id(category.getId(), exhibition.getId())).thenReturn(Optional.of(category));
        when(mediaService.uploadForItem(item, file, "image")).thenReturn(replacement);
        service.update(item.getId(), exhibition.getId(), request(category.getId(), null), file, null, null, user(UserType.ADMIN));
        assertThat(item.getThumbnailMedia()).isSameAs(replacement);
        verifyNoInteractions(mappings);
    }

    /** 다른 전시회의 분류를 지정하면 작품 변경 전 거부한다. */
    @Test
    void update_rejectsForeignClassificationBeforeMutation() {
        ExhibitionEntity exhibition = new ExhibitionEntity(UUID.randomUUID(), "slug", "전시");
        CategoryEntity category = new CategoryEntity(UUID.randomUUID(), exhibition, "AI");
        ItemEntity item = new ItemEntity(UUID.randomUUID(), exhibition, category, null, "기존", null);
        when(items.findByIdAndExhibition_Id(item.getId(), exhibition.getId())).thenReturn(Optional.of(item));
        UUID foreign = UUID.randomUUID();
        when(classifications.findByIdAndExhibition_Id(foreign, exhibition.getId())).thenReturn(Optional.empty());
        var request = new AdminItemRequest(category.getId(), null, "수정", null, null, null, null, false,
                List.of(foreign), false, false, false);
        assertThatThrownBy(() -> service.update(item.getId(), exhibition.getId(), request, user(UserType.ADMIN)))
                .isInstanceOf(NotFoundException.class);
        assertThat(item.getTitle()).isEqualTo("기존");
        verifyNoInteractions(categories, mediaService, mappings);
    }

    private AdminItemRequest request(UUID categoryId, UUID periodId) {
        return new AdminItemRequest(categoryId, periodId, "작품", "설명", "학생", "a@example.com", "교수", false);
    }

    private AuthenticatedUser user(UserType role) {
        return new AuthenticatedUser(UUID.randomUUID(), "user", role, null, null, null, null, true);
    }
}
