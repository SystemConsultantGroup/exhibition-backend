package kr.ac.skku.scg.exhibition.item.service;

import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import java.util.List;
import java.util.HashSet;
import kr.ac.skku.scg.exhibition.classification.domain.ItemClassificationEntity;
import kr.ac.skku.scg.exhibition.classification.domain.ItemClassificationMapEntity;
import kr.ac.skku.scg.exhibition.classification.repository.ItemClassificationRepository;
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
import kr.ac.skku.scg.exhibition.media.service.MediaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

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
    private final MediaService mediaService;
    private final ItemClassificationRepository classifications;

    public AdminItemService(ItemRepository items, CategoryRepository categories, EventPeriodRepository periods,
            ItemLikeRepository likes, ItemClassificationMapRepository mappings, MediaAssetRepository media,
            AdminAuthorizationService authorization, MinioClient minio, MinioProperties minioProperties,
            MediaService mediaService, ItemClassificationRepository classifications) {
        this.items = items; this.categories = categories; this.periods = periods; this.likes = likes;
        this.mappings = mappings; this.media = media; this.authorization = authorization;
        this.minio = minio; this.minioProperties = minioProperties; this.mediaService = mediaService;
        this.classifications = classifications;
    }

    /** 미디어 파일과 함께 아이템을 생성한다. */
    @Transactional
    public ItemResponse create(ExhibitionEntity exhibition, AdminItemRequest request, MultipartFile thumbnailFile,
            MultipartFile posterFile, MultipartFile presentationVideoFile, AuthenticatedUser user) {
        authorization.requireExhibitionAdmin(user, exhibition.getId());
        CategoryEntity category = category(request.categoryId(), exhibition.getId());
        EventPeriodEntity period = period(request.eventPeriodId(), exhibition.getId());
        ItemEntity item = new ItemEntity(null, exhibition, category, period, request.title().trim(), request.description());
        item.updateMetadata(category, period, request.title().trim(), request.description(), request.participantNames(),
                request.participantEmails(), request.advisorNames(), request.awarded());
        List<ItemClassificationEntity> selected = classifications(request.classificationIds(), exhibition.getId());
        item = items.save(item);
        updateClassifications(item, selected);
        item.updateMedia(
                mediaService.uploadForItem(item, thumbnailFile, "image"),
                mediaService.uploadForItem(item, posterFile, "image"),
                mediaService.uploadForItem(item, presentationVideoFile, "video")
        );
        return response(item);
    }

    /** JSON 수정은 파일 없이 같은 검증 및 수정 경로를 사용한다. */
    @Transactional
    public ItemResponse update(UUID id, UUID exhibitionId, AdminItemRequest request, AuthenticatedUser user) {
        return update(id, exhibitionId, request, null, null, null, user);
    }

    /** 메타데이터와 분류, 미디어 교체·제거를 한 트랜잭션으로 반영한다. */
    @Transactional
    public ItemResponse update(UUID id, UUID exhibitionId, AdminItemRequest request,
            MultipartFile thumbnailFile, MultipartFile posterFile, MultipartFile presentationVideoFile,
            AuthenticatedUser user) {
        authorization.requireExhibitionAdmin(user, exhibitionId);
        ItemEntity item = item(id, exhibitionId);
        List<ItemClassificationEntity> selected = classifications(request.classificationIds(), exhibitionId);
        validateReplacement(thumbnailFile, request.removeThumbnail());
        validateReplacement(posterFile, request.removePoster());
        validateReplacement(presentationVideoFile, request.removePresentationVideo());
        item.updateMetadata(category(request.categoryId(), exhibitionId), period(request.eventPeriodId(), exhibitionId),
                request.title().trim(), request.description(), request.participantNames(), request.participantEmails(),
                request.advisorNames(), request.awarded());
        item.updateMedia(
                replaceMedia(item, thumbnailFile, request.removeThumbnail(), item.getThumbnailMedia(), "image"),
                replaceMedia(item, posterFile, request.removePoster(), item.getPosterMedia(), "image"),
                replaceMedia(item, presentationVideoFile, request.removePresentationVideo(), item.getPresentationVideoMedia(), "video"));
        updateClassifications(item, selected);
        return response(item);
    }

    /** 교체와 제거를 동시에 요청하는 모호한 입력을 거부한다. */
    private void validateReplacement(MultipartFile file, boolean remove) {
        if (remove && file != null && !file.isEmpty()) {
            throw new IllegalArgumentException("Cannot replace and remove the same media");
        }
    }

    /** 파일이 생략되면 기존 미디어를 유지하며 명시적 제거만 반영한다. */
    private MediaAssetEntity replaceMedia(ItemEntity item, MultipartFile file, boolean remove,
            MediaAssetEntity existing, String type) {
        return file != null && !file.isEmpty() ? mediaService.uploadForItem(item, file, type) : remove ? null : existing;
    }

    /** 변경 전에 분류 ID를 중복 및 테넌트 범위까지 검증한다. */
    private List<ItemClassificationEntity> classifications(List<UUID> ids, UUID exhibitionId) {
        if (ids == null) return null;
        if (new HashSet<>(ids).size() != ids.size() || ids.stream().anyMatch(java.util.Objects::isNull)) {
            throw new IllegalArgumentException("classificationIds must contain unique non-null IDs");
        }
        return ids.stream().map(id -> classifications.findByIdAndExhibition_Id(id, exhibitionId)
                .orElseThrow(() -> new NotFoundException("Classification not found: " + id))).toList();
    }

    /** 분류 생략은 유지, 빈 목록은 해제, 나머지는 전체 교체한다. */
    private void updateClassifications(ItemEntity item, List<ItemClassificationEntity> selected) {
        if (selected == null) return;
        mappings.deleteAllByItem_Id(item.getId());
        mappings.flush();
        mappings.saveAll(selected.stream().map(value -> new ItemClassificationMapEntity(null, item, value)).toList());
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
