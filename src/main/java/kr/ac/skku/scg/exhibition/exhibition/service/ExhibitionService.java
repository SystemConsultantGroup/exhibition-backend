package kr.ac.skku.scg.exhibition.exhibition.service;

import java.util.List;
import java.util.UUID;
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
        UUID logoMediaId = exhibition.getLogoMedia() == null ? null : exhibition.getLogoMedia().getId();
        UUID bannerMediaId = exhibition.getBannerMedia() == null ? null : exhibition.getBannerMedia().getId();
        UUID popupImageMediaId = exhibition.getPopupImageMedia() == null ? null : exhibition.getPopupImageMedia().getId();
        UUID introVideoMediaId = exhibition.getIntroVideoMedia() == null ? null : exhibition.getIntroVideoMedia().getId();

        return new ExhibitionResponse(
                exhibition.getId(),
                exhibition.getSlug(),
                exhibition.getDefaultDomain(),
                exhibition.getCustomDomain(),
                exhibition.getName(),
                exhibition.getDescription(),
                logoMediaId,
                exhibition.isBannerEnabled(),
                bannerMediaId,
                exhibition.isPopupEnabled(),
                popupImageMediaId,
                exhibition.getPopupUrl(),
                exhibition.getIntroTitle(),
                exhibition.getIntroDescription(),
                introVideoMediaId);
    }
}
