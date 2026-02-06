package kr.ac.skku.scg.exhibition.item.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.ac.skku.scg.exhibition.exhibition.service.ExhibitionService;
import kr.ac.skku.scg.exhibition.global.error.NotFoundException;
import kr.ac.skku.scg.exhibition.item.domain.ItemEntity;
import kr.ac.skku.scg.exhibition.item.domain.ItemVisibility;
import kr.ac.skku.scg.exhibition.item.repository.ItemRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemService {

    private final ItemRepository itemRepository;
    private final ExhibitionService exhibitionService;

    public Page<ItemEntity> list(
        UUID exhibitionId,
        UUID categoryId,
        String q,
        String visibility,
        Boolean published,
        String classification,
        Pageable pageable
    ) {
        exhibitionService.get(exhibitionId);
        ItemVisibility parsedVisibility = visibility == null ? null : ItemVisibility.from(visibility);
        return itemRepository.search(exhibitionId, categoryId, q, parsedVisibility, published, classification, pageable);
    }

    public ItemEntity get(UUID itemId) {
        return itemRepository.findById(itemId)
            .orElseThrow(() -> new NotFoundException("Item not found"));
    }
}
