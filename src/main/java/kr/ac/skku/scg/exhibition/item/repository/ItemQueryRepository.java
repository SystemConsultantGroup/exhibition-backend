package kr.ac.skku.scg.exhibition.item.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import kr.ac.skku.scg.exhibition.item.domain.ItemEntity;
import kr.ac.skku.scg.exhibition.item.domain.ItemVisibility;

public interface ItemQueryRepository {

    Page<ItemEntity> search(
        UUID exhibitionId,
        UUID categoryId,
        String q,
        ItemVisibility visibility,
        Boolean published,
        String classification,
        Pageable pageable
    );
}
