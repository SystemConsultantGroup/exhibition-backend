package kr.ac.skku.scg.exhibition.global.tenant;

import java.util.Optional;
import kr.ac.skku.scg.exhibition.exhibition.domain.ExhibitionEntity;
import kr.ac.skku.scg.exhibition.exhibition.repository.ExhibitionRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/** 도메인 기반 전시 조회 결과를 캐싱하여 요청당 중복 DB 조회를 방지합니다. */
@Service
public class ExhibitionDomainCacheService {

    private final ExhibitionRepository exhibitionRepository;

    public ExhibitionDomainCacheService(ExhibitionRepository exhibitionRepository) {
        this.exhibitionRepository = exhibitionRepository;
    }

    /**
     * 호스트 도메인으로 전시를 조회합니다.
     * 조회 결과를 캐시하여 동일 도메인에 대한 반복 DB 접근을 줄입니다.
     * 전시가 존재하지 않는 경우는 캐시하지 않습니다.
     */
    @Cacheable(value = "exhibition-by-domain", unless = "#result.isEmpty()")
    public Optional<ExhibitionEntity> findByDomain(String host) {
        return exhibitionRepository.findFirstByDefaultDomainIgnoreCaseOrCustomDomainIgnoreCase(host, host);
    }
}
