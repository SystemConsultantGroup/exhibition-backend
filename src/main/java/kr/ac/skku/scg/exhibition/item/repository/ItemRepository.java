package kr.ac.skku.scg.exhibition.item.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import kr.ac.skku.scg.exhibition.item.domain.ItemEntity;

public interface ItemRepository extends JpaRepository<ItemEntity, UUID>, ItemQueryRepository {
}
