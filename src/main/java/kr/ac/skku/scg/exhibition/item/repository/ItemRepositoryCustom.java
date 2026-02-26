package kr.ac.skku.scg.exhibition.item.repository;

import kr.ac.skku.scg.exhibition.item.domain.ItemEntity;
import kr.ac.skku.scg.exhibition.item.dto.request.ItemListRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ItemRepositoryCustom {

    Page<ItemEntity> search(ItemListRequest request, Pageable pageable);
}
