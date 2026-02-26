package kr.ac.skku.scg.exhibition.item.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import kr.ac.skku.scg.exhibition.global.auth.resolver.AuthenticatedUser;
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

    public ItemResponse get(UUID id) {
        ItemEntity item = itemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Item not found: " + id));
        long likes = itemLikeRepository.countByItem_Id(id);
        return toResponse(item, likes);
    }

    public List<ItemResponse> list(ItemListRequest request) {
        List<ItemEntity> items;

        if (request.getCategoryId() != null) {
            items = itemRepository.findAllByExhibition_IdAndCategory_Id(request.getExhibitionId(), request.getCategoryId());
        } else if (request.getEventPeriodId() != null) {
            items = itemRepository.findAllByExhibition_IdAndEventPeriod_Id(request.getExhibitionId(), request.getEventPeriodId());
        } else {
            items = itemRepository.findAllByExhibition_Id(request.getExhibitionId());
        }

        if (items.isEmpty()) {
            return List.of();
        }

        Map<UUID, Long> likesByItemId = itemLikeRepository.countLikesByItemIds(
                        items.stream().map(ItemEntity::getId).toList()).stream()
                .collect(toMap(ItemLikeRepository.ItemLikeCount::getItemId, ItemLikeRepository.ItemLikeCount::getLikeCount));

        return items.stream()
                .map(item -> toResponse(item, likesByItemId.getOrDefault(item.getId(), 0L)))
                .toList();
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

    private ItemResponse toResponse(ItemEntity item, long likes) {
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
                likes);
    }
}
