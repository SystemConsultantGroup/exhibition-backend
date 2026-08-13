package kr.ac.skku.scg.exhibition.item.service;

import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import java.util.List;
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
import kr.ac.skku.scg.exhibition.global.error.NotFoundException;
import kr.ac.skku.scg.exhibition.item.domain.ItemEntity;
import kr.ac.skku.scg.exhibition.item.dto.request.AdminItemRequest;
import kr.ac.skku.scg.exhibition.item.dto.response.ItemResponse;
import kr.ac.skku.scg.exhibition.item.repository.ItemLikeRepository;
import kr.ac.skku.scg.exhibition.item.repository.ItemRepository;
import kr.ac.skku.scg.exhibition.media.domain.MediaAssetEntity;
import kr.ac.skku.scg.exhibition.media.repository.MediaAssetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class AdminItemService {
    private final ItemRepository items;
    private final CategoryRepository categories;
    private final EventPeriodRepository periods;
    private final ItemLikeRepository likes;
    private final ItemClassificationMapRepository mappings;
    private final MediaAssetRepository media;
    private final AdminAuthorizationService authorization;
    private final MinioClient minio;
    private final MinioProperties minioProperties;

    public AdminItemService(ItemRepository items, CategoryRepository categories, EventPeriodRepository periods,
            ItemLikeRepository likes, ItemClassificationMapRepository mappings, MediaAssetRepository media,
            AdminAuthorizationService authorization, MinioClient minio, MinioProperties minioProperties) {
        this.items = items; this.categories = categories; this.periods = periods; this.likes = likes;
        this.mappings = mappings; this.media = media; this.authorization = authorization;
        this.minio = minio; this.minioProperties = minioProperties;
    }

    /** 미디어 없이 아이템을 생성한다. */
    @Transactional
    public ItemResponse create(ExhibitionEntity exhibition, AdminItemRequest request, AuthenticatedUser user) {
        authorization.requireExhibitionAdmin(user, exhibition.getId());
        CategoryEntity category = category(request.categoryId(), exhibition.getId());
        EventPeriodEntity period = period(request.eventPeriodId(), exhibition.getId());
        ItemEntity item = new ItemEntity(null, exhibition, category, period, request.title().trim(), request.description());
        item.updateMetadata(category, period, request.title().trim(), request.description(), request.participantNames(),
                request.participantEmails(), request.advisorNames(), request.awarded());
        return response(items.save(item));
    }

    /** 아이템 메타데이터를 수정하고 기존 미디어 연결은 유지한다. */
    @Transactional
    public ItemResponse update(UUID id, UUID exhibitionId, AdminItemRequest request, AuthenticatedUser user) {
        authorization.requireExhibitionAdmin(user, exhibitionId);
        ItemEntity item = item(id, exhibitionId);
        item.updateMetadata(category(request.categoryId(), exhibitionId), period(request.eventPeriodId(), exhibitionId),
                request.title().trim(), request.description(), request.participantNames(), request.participantEmails(),
                request.advisorNames(), request.awarded());
        return response(item);
    }

    /** 아이템과 종속 관계 및 소유 미디어를 삭제한다. */
    @Transactional
    public void delete(UUID id, UUID exhibitionId, AuthenticatedUser user) {
        authorization.requireExhibitionAdmin(user, exhibitionId);
        ItemEntity item = item(id, exhibitionId);
        List<String> objectKeys = media.findAllByItem_Id(id).stream().map(MediaAssetEntity::getObjectKey).toList();
        item.updateMedia(null, null, null);
        items.flush();
        likes.deleteAllByItem_Id(id);
        mappings.deleteAllByItem_Id(id);
        media.deleteAllByItem_Id(id);
        items.delete(item);
        removeAfterCommit(objectKeys);
    }

    /** 전시회 범위에서 아이템을 찾는다. */
    private ItemEntity item(UUID id, UUID exhibitionId) {
        return items.findByIdAndExhibition_Id(id, exhibitionId).orElseThrow(() -> new NotFoundException("Item not found: " + id));
    }

    /** 전시회 범위에서 카테고리를 찾는다. */
    private CategoryEntity category(UUID id, UUID exhibitionId) {
        return categories.findByIdAndExhibition_Id(id, exhibitionId).orElseThrow(() -> new NotFoundException("Category not found: " + id));
    }

    /** 선택된 발표회차를 전시회 범위에서 찾는다. */
    private EventPeriodEntity period(UUID id, UUID exhibitionId) {
        return id == null ? null : periods.findByIdAndExhibition_Id(id, exhibitionId)
                .orElseThrow(() -> new NotFoundException("Event period not found: " + id));
    }

    /** 관리자용 아이템 응답을 만든다. */
    private ItemResponse response(ItemEntity item) {
        return new ItemResponse(item.getId(), item.getExhibition().getId(), item.getCategory().getId(),
                item.getEventPeriod() == null ? null : item.getEventPeriod().getId(), item.getTitle(), item.getDescription(),
                item.getParticipantNames(), item.getParticipantEmails(), item.getAdvisorNames(),
                item.getThumbnailMedia() == null ? null : item.getThumbnailMedia().getId(),
                item.getPosterMedia() == null ? null : item.getPosterMedia().getId(),
                item.getPresentationVideoMedia() == null ? null : item.getPresentationVideoMedia().getId(),
                item.isAwarded(), likes.countByItem_Id(item.getId()), false);
    }

    /** DB 커밋 후 MinIO 객체를 최선 노력으로 삭제한다. */
    private void removeAfterCommit(List<String> keys) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                for (String key : keys) try {
                    minio.removeObject(RemoveObjectArgs.builder().bucket(minioProperties.getBucket()).object(key).build());
                } catch (Exception ignored) { }
            }
        });
    }
}
