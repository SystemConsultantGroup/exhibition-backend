package kr.ac.skku.scg.exhibition.item.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import kr.ac.skku.scg.exhibition.category.domain.CategoryEntity;
import kr.ac.skku.scg.exhibition.category.repository.CategoryRepository;
import kr.ac.skku.scg.exhibition.classification.domain.ItemClassificationEntity;
import kr.ac.skku.scg.exhibition.classification.domain.ItemClassificationMapEntity;
import kr.ac.skku.scg.exhibition.classification.repository.ItemClassificationMapRepository;
import kr.ac.skku.scg.exhibition.classification.repository.ItemClassificationRepository;
import kr.ac.skku.scg.exhibition.eventperiod.domain.EventPeriodEntity;
import kr.ac.skku.scg.exhibition.eventperiod.repository.EventPeriodRepository;
import kr.ac.skku.scg.exhibition.exhibition.domain.ExhibitionEntity;
import kr.ac.skku.scg.exhibition.exhibition.repository.ExhibitionRepository;
import kr.ac.skku.scg.exhibition.item.domain.ItemEntity;
import kr.ac.skku.scg.exhibition.item.dto.request.ItemListRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@DataJpaTest
class ItemRepositoryTest {

    @Autowired
    private ExhibitionRepository exhibitionRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private EventPeriodRepository eventPeriodRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private ItemClassificationRepository classificationRepository;

    @Autowired
    private ItemClassificationMapRepository classificationMapRepository;

    private UUID exhibitionId;
    private UUID eventPeriodId;
    private UUID classificationId;

    @BeforeEach
    void setUp() {
        ExhibitionEntity exhibition = exhibitionRepository.save(new ExhibitionEntity(null, "exhibition-1", "전시 1"));
        CategoryEntity category = categoryRepository.save(new CategoryEntity(null, exhibition, "소프트웨어"));

        EventPeriodEntity period2025_1 = eventPeriodRepository.save(new EventPeriodEntity(
                null,
                exhibition,
                "2025-1",
                Instant.parse("2025-03-01T00:00:00Z"),
                Instant.parse("2025-06-30T23:59:59Z")
        ));

        EventPeriodEntity period2026_2 = eventPeriodRepository.save(new EventPeriodEntity(
                null,
                exhibition,
                "2026-2",
                Instant.parse("2026-09-01T00:00:00Z"),
                Instant.parse("2026-12-31T23:59:59Z")
        ));

        ItemEntity itemA = new ItemEntity(null, exhibition, category, period2025_1, "Alpha", "desc-a");
        itemA.updateParticipantAndAdvisor("홍길동", "hong@example.com", "김교수");
        itemRepository.save(itemA);

        ItemEntity itemB = new ItemEntity(null, exhibition, category, period2026_2, "Beta", "desc-b");
        itemB.updateParticipantAndAdvisor("이몽룡", "lee@example.com", "박교수");
        itemRepository.save(itemB);

        ItemClassificationEntity classification = classificationRepository.save(
                new ItemClassificationEntity(null, exhibition, "AI"));
        classificationMapRepository.save(new ItemClassificationMapEntity(null, itemB, classification));

        exhibitionId = exhibition.getId();
        eventPeriodId = period2026_2.getId();
        classificationId = classification.getId();
    }

    @Test
    void search_filtersByClassificationEventPeriodAndQuery() {
        ItemListRequest request = new ItemListRequest();
        request.setExhibitionId(exhibitionId);
        request.setClassificationId(classificationId);
        request.setEventPeriodId(eventPeriodId);
        request.setQ("박교수");

        Page<ItemEntity> page = itemRepository.search(request, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getTitle()).isEqualTo("Beta");
    }

    @Test
    void search_sortsByTitleAscending() {
        ItemListRequest request = new ItemListRequest();
        request.setExhibitionId(exhibitionId);

        Page<ItemEntity> page = itemRepository.search(request, PageRequest.of(0, 10, Sort.by(Sort.Order.asc("title"))));

        assertThat(page.getContent()).extracting(ItemEntity::getTitle).containsExactly("Alpha", "Beta");
    }
}
