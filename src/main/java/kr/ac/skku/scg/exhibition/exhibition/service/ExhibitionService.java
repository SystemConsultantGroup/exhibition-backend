package kr.ac.skku.scg.exhibition.exhibition.service;

import java.util.List;
import kr.ac.skku.scg.exhibition.exhibition.domain.ExhibitionEntity;
import kr.ac.skku.scg.exhibition.exhibition.dto.request.ExhibitionListRequest;
import kr.ac.skku.scg.exhibition.exhibition.dto.response.ExhibitionResponse;
import kr.ac.skku.scg.exhibition.exhibition.dto.response.ExhibitionSlugResponse;
import kr.ac.skku.scg.exhibition.exhibition.repository.ExhibitionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
public class ExhibitionService {

    private final ExhibitionRepository exhibitionRepository;

    public ExhibitionService(ExhibitionRepository exhibitionRepository) {
        this.exhibitionRepository = exhibitionRepository;
    }

    public ExhibitionResponse get(ExhibitionEntity currentExhibition) {
        return toResponse(currentExhibition);
    }

    public List<ExhibitionResponse> list(ExhibitionListRequest request, ExhibitionEntity currentExhibition) {
        String keyword = request.getQ();
        String slug = request.getSlug();

        if (StringUtils.hasText(slug) && !slug.trim().equals(currentExhibition.getSlug())) {
            return List.of();
        }
        if (StringUtils.hasText(keyword)
                && !currentExhibition.getName().toLowerCase().contains(keyword.trim().toLowerCase())) {
            return List.of();
        }
        return List.of(toResponse(currentExhibition));
    }

    public ExhibitionSlugResponse getSlug(ExhibitionEntity currentExhibition) {
        return new ExhibitionSlugResponse(currentExhibition.getSlug());
    }

    private ExhibitionResponse toResponse(ExhibitionEntity exhibition) {
        return ExhibitionResponse.from(exhibition);
    }
}
