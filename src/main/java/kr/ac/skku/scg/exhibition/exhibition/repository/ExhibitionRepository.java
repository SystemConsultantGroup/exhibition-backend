package kr.ac.skku.scg.exhibition.exhibition.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import kr.ac.skku.scg.exhibition.exhibition.domain.ExhibitionServiceEntity;

public interface ExhibitionRepository extends JpaRepository<ExhibitionServiceEntity, UUID> {

    Page<ExhibitionServiceEntity> findByIsActive(boolean isActive, Pageable pageable);
}
