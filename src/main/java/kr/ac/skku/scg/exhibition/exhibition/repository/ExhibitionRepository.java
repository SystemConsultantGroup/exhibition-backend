package kr.ac.skku.scg.exhibition.exhibition.repository;

import java.util.Optional;
import java.util.UUID;
import kr.ac.skku.scg.exhibition.exhibition.domain.ExhibitionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExhibitionRepository extends JpaRepository<ExhibitionEntity, UUID> {

    Optional<ExhibitionEntity> findBySlug(String slug);

    Optional<ExhibitionEntity> findFirstByDefaultDomainIgnoreCaseOrCustomDomainIgnoreCase(
            String defaultDomain,
            String customDomain
    );

    boolean existsByDefaultDomainIgnoreCaseOrCustomDomainIgnoreCase(String defaultDomain, String customDomain);
}
