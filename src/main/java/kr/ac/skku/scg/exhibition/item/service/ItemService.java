package kr.ac.skku.scg.exhibition.item.service;

import java.util.List;
import java.util.UUID;
import kr.ac.skku.scg.exhibition.global.error.NotFoundException;
import kr.ac.skku.scg.exhibition.item.domain.ItemEntity;
import kr.ac.skku.scg.exhibition.item.dto.request.ItemListRequest;
import kr.ac.skku.scg.exhibition.item.dto.response.ItemResponse;
import kr.ac.skku.scg.exhibition.item.repository.ItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ItemService {

    private final ItemRepository itemRepository;

    public ItemService(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    public ItemResponse get(UUID id) {
        ItemEntity item = itemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Item not found: " + id));
        return toResponse(item);
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

        return items.stream().map(this::toResponse).toList();
    }

    private ItemResponse toResponse(ItemEntity item) {
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
                item.getAdvisorNames(),
                thumbnailMediaId,
                posterMediaId,
                presentationVideoMediaId);
    }
}
