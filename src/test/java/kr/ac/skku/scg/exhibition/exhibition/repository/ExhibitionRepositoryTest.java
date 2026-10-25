package kr.ac.skku.scg.exhibition.exhibition.repository;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import kr.ac.skku.scg.exhibition.exhibition.domain.ExhibitionEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
class ExhibitionRepositoryTest {

    @Autowired
    private ExhibitionRepository exhibitionRepository;

    @Autowired
    private EntityManager entityManager;

    /** 신규 엔티티의 기본 표시 설정을 H2에 저장하고 다시 조회한다. */
    @Test
    void save_persistsDefaultDisplaySettings() {
        ExhibitionEntity exhibition = exhibitionRepository.saveAndFlush(new ExhibitionEntity(null, "default", "기본 전시"));
        entityManager.clear();

        ExhibitionEntity reloaded = exhibitionRepository.findById(exhibition.getId()).orElseThrow();

        assertThat(reloaded.isShowAwards()).isTrue();
        assertThat(reloaded.isDefaultToLatestEventPeriod()).isFalse();
    }

    /** 표시 설정의 모든 조합을 저장하고 영속성 컨텍스트 밖에서 다시 읽는다. */
    @ParameterizedTest
    @CsvSource({"true,false", "false,true", "true,true", "false,false"})
    void save_persistsCustomDisplaySettings(boolean showAwards, boolean defaultToLatestEventPeriod) {
        ExhibitionEntity exhibition = new ExhibitionEntity(null, "custom", "설정 전시");
        exhibition.updateDisplaySettings(showAwards, defaultToLatestEventPeriod);
        exhibitionRepository.saveAndFlush(exhibition);
        entityManager.clear();

        ExhibitionEntity reloaded = exhibitionRepository.findById(exhibition.getId()).orElseThrow();

        assertThat(reloaded.isShowAwards()).isEqualTo(showAwards);
        assertThat(reloaded.isDefaultToLatestEventPeriod()).isEqualTo(defaultToLatestEventPeriod);
    }
}
