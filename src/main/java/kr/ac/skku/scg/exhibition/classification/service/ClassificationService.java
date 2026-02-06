package kr.ac.skku.scg.exhibition.classification.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.ac.skku.scg.exhibition.classification.domain.ItemClassificationEntity;
import kr.ac.skku.scg.exhibition.classification.repository.ItemClassificationRepository;
import kr.ac.skku.scg.exhibition.exhibition.service.ExhibitionService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClassificationService {

    private final ItemClassificationRepository classificationRepository;
    private final ExhibitionService exhibitionService;

    public List<ItemClassificationEntity> list(UUID exhibitionId) {
        exhibitionService.get(exhibitionId);
        return classificationRepository.findByExhibitionIdOrderByNameAsc(exhibitionId);
    }
}
