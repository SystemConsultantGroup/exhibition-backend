package kr.ac.skku.scg.exhibition.exhibition.repository;

import java.util.Optional;
import java.util.UUID;
import kr.ac.skku.scg.exhibition.exhibition.domain.ExhibitionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExhibitionRepository extends JpaRepository<ExhibitionEntity, UUID> {

    Optional<ExhibitionEntity> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, UUID id);

    @Query("""
            select count(e) > 0 from ExhibitionEntity e
            where e.id <> :id
              and (lower(e.defaultDomain) = lower(:domain) or lower(e.customDomain) = lower(:domain))
            """)
    boolean existsByDomainUsedByAnotherExhibition(@Param("domain") String domain, @Param("id") UUID id);

    Optional<ExhibitionEntity> findFirstByDefaultDomainIgnoreCaseOrCustomDomainIgnoreCase(
            String defaultDomain,
            String customDomain
    );

    boolean existsByDefaultDomainIgnoreCaseOrCustomDomainIgnoreCase(String defaultDomain, String customDomain);
}
