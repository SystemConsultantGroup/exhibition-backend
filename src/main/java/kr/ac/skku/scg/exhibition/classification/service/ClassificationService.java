package kr.ac.skku.scg.exhibition.classification.service;

import java.util.List;
import java.util.UUID;
import kr.ac.skku.scg.exhibition.classification.domain.ItemClassificationEntity;
import kr.ac.skku.scg.exhibition.classification.dto.request.ClassificationListRequest;
import kr.ac.skku.scg.exhibition.classification.dto.response.ClassificationResponse;
import kr.ac.skku.scg.exhibition.classification.repository.ItemClassificationRepository;
import kr.ac.skku.scg.exhibition.global.error.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ClassificationService {

    private final ItemClassificationRepository itemClassificationRepository;

    public ClassificationService(ItemClassificationRepository itemClassificationRepository) {
        this.itemClassificationRepository = itemClassificationRepository;
    }

    public ClassificationResponse get(UUID id) {
        ItemClassificationEntity classification = itemClassificationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Classification not found: " + id));
        return toResponse(classification);
    }

    public List<ClassificationResponse> list(ClassificationListRequest request) {
        return itemClassificationRepository.findAllByExhibition_Id(request.getExhibitionId()).stream()
                .map(this::toResponse)
                .toList();
    }

    private ClassificationResponse toResponse(ItemClassificationEntity classification) {
        return new ClassificationResponse(
                classification.getId(),
                classification.getExhibition().getId(),
                classification.getName(),
                classification.getCreatedAt());
    }
}
