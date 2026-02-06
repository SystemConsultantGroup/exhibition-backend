package kr.ac.skku.scg.exhibition.exhibition.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.ac.skku.scg.exhibition.exhibition.domain.ExhibitionServiceEntity;
import kr.ac.skku.scg.exhibition.exhibition.repository.ExhibitionRepository;
import kr.ac.skku.scg.exhibition.global.error.NotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExhibitionService {

    private final ExhibitionRepository exhibitionRepository;

    public Page<ExhibitionServiceEntity> list(Boolean active, Pageable pageable) {
        if (active == null) {
            return exhibitionRepository.findAll(pageable);
        }
        return exhibitionRepository.findByIsActive(active, pageable);
    }

    public ExhibitionServiceEntity get(UUID exhibitionId) {
        return exhibitionRepository.findById(exhibitionId)
            .orElseThrow(() -> new NotFoundException("Exhibition not found"));
    }
}
