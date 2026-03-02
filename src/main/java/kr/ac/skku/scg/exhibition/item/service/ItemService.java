package kr.ac.skku.scg.exhibition.item.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import kr.ac.skku.scg.exhibition.global.auth.resolver.AuthenticatedUser;
import kr.ac.skku.scg.exhibition.global.dto.ListResponse;
import kr.ac.skku.scg.exhibition.global.error.NotFoundException;
import kr.ac.skku.scg.exhibition.item.domain.ItemEntity;
import kr.ac.skku.scg.exhibition.item.domain.ItemLikeEntity;
import kr.ac.skku.scg.exhibition.item.dto.request.ItemListRequest;
import kr.ac.skku.scg.exhibition.item.dto.response.ItemResponse;
import kr.ac.skku.scg.exhibition.item.repository.ItemLikeRepository;
import kr.ac.skku.scg.exhibition.item.repository.ItemRepository;
import kr.ac.skku.scg.exhibition.user.domain.UserEntity;
import kr.ac.skku.scg.exhibition.user.repository.UserRepository;
import static java.util.stream.Collectors.toMap;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ItemService {

    private final ItemRepository itemRepository;
    private final ItemLikeRepository itemLikeRepository;
    private final UserRepository userRepository;

    public ItemService(ItemRepository itemRepository, ItemLikeRepository itemLikeRepository, UserRepository userRepository) {
        this.itemRepository = itemRepository;
        this.itemLikeRepository = itemLikeRepository;
        this.userRepository = userRepository;
    }

    public ItemResponse get(UUID id, @Nullable UUID currentUserId) {
        ItemEntity item = itemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Item not found: " + id));
        long likes = itemLikeRepository.countByItem_Id(id);
        boolean isLike = currentUserId != null && itemLikeRepository.existsByItem_IdAndUser_Id(id, currentUserId);
        return toResponse(item, likes, isLike);
    }

    public ListResponse<ItemResponse> list(ItemListRequest request, Pageable pageable, @Nullable UUID currentUserId) {
        Page<ItemEntity> page = itemRepository.search(request, pageable);
        List<ItemEntity> items = page.getContent();
        if (items.isEmpty()) {
            return new ListResponse<>(List.of(), page.getNumber() + 1, page.getSize(), page.getTotalElements());
        }

        List<UUID> itemIds = items.stream().map(ItemEntity::getId).toList();

        Map<UUID, Long> likesByItemId = itemLikeRepository.countLikesByItemIds(
                        itemIds).stream()
                .collect(toMap(ItemLikeRepository.ItemLikeCount::getItemId, ItemLikeRepository.ItemLikeCount::getLikeCount));

        Set<UUID> likedItemIds = currentUserId == null
                ? Set.of()
                : Set.copyOf(itemLikeRepository.findLikedItemIdsByUserIdAndItemIds(currentUserId, itemIds));

        List<ItemResponse> responses = items.stream()
                .map(item -> toResponse(
                        item,
                        likesByItemId.getOrDefault(item.getId(), 0L),
                        likedItemIds.contains(item.getId())))
                .toList();

        return new ListResponse<>(responses, page.getNumber() + 1, page.getSize(), page.getTotalElements());
    }

    @Transactional
    public void like(UUID itemId, AuthenticatedUser currentUser) {
        ItemEntity item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Item not found: " + itemId));

        if (itemLikeRepository.existsByItem_IdAndUser_Id(itemId, currentUser.id())) {
            return;
        }

        UserEntity user = userRepository.getReferenceById(currentUser.id());
        try {
            itemLikeRepository.save(new ItemLikeEntity(null, item, user));
        } catch (DataIntegrityViolationException ignored) {
            // Race condition safety: unique constraint already guarantees one like per user/item.
        }
    }

    @Transactional
    public void unlike(UUID itemId, AuthenticatedUser currentUser) {
        if (!itemRepository.existsById(itemId)) {
            throw new NotFoundException("Item not found: " + itemId);
        }

        itemLikeRepository.deleteByItem_IdAndUser_Id(itemId, currentUser.id());
    }

    private ItemResponse toResponse(ItemEntity item, long likes, boolean isLike) {
        UUID eventPeriodId = item.getEventPeriod() == null ? null : item.getEventPeriod().getId();
        UUID thumbnailMediaId = item.getThumbnailMedia() == null ? null : item.getThumbnailMedia().getId();
        UUID posterMediaId = item.getPosterMedia() == null ? null : item.getPosterMedia().getId();
        UUID presentationVideoMediaId = item.getPresentationVideoMedia() == null ? null : item.getPresentationVideoMedia().getId();

        return new ItemResponse(
                item.getId(),
                item.getExhibition().getId(),
                item.getCategory().getId(),
                eventPeriodId,
                item.getTitle(),
                item.getDescription(),
                item.getParticipantNames(),
                item.getParticipantEmails(),
                item.getAdvisorNames(),
                thumbnailMediaId,
                posterMediaId,
                presentationVideoMediaId,
                likes,
                isLike);
    }
}
