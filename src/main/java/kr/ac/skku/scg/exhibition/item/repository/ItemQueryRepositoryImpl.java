package kr.ac.skku.scg.exhibition.item.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;

import jakarta.persistence.EntityManager;
import kr.ac.skku.scg.exhibition.classification.domain.ItemClassificationEntity;
import kr.ac.skku.scg.exhibition.classification.domain.ItemClassificationMapEntity;
import kr.ac.skku.scg.exhibition.item.domain.ItemEntity;
import kr.ac.skku.scg.exhibition.item.domain.ItemVisibility;

@Repository
public class ItemQueryRepositoryImpl implements ItemQueryRepository {

    private final JPAQueryFactory queryFactory;

    public ItemQueryRepositoryImpl(EntityManager entityManager) {
        this.queryFactory = new JPAQueryFactory(entityManager);
    }

    @Override
    public Page<ItemEntity> search(
        UUID exhibitionId,
        UUID categoryId,
        String q,
        ItemVisibility visibility,
        Boolean published,
        String classification,
        Pageable pageable
    ) {
        PathBuilder<ItemEntity> item = new PathBuilder<>(ItemEntity.class, "item");
        PathBuilder<ItemClassificationMapEntity> icm = new PathBuilder<>(ItemClassificationMapEntity.class, "icm");
        PathBuilder<ItemClassificationEntity> cls = new PathBuilder<>(ItemClassificationEntity.class, "cls");

        BooleanBuilder where = new BooleanBuilder();
        where.and(item.get("exhibition").get("id", UUID.class).eq(exhibitionId));

        if (categoryId != null) {
            where.and(item.get("category").get("id", UUID.class).eq(categoryId));
        }
        if (q != null && !q.isBlank()) {
            where.and(item.getString("title").containsIgnoreCase(q));
        }
        if (visibility != null) {
            where.and(item.getEnum("visibility", ItemVisibility.class).eq(visibility));
        }
        if (published != null) {
            if (published) {
                where.and(item.getDateTime("publishedAt", java.time.Instant.class).isNotNull());
            } else {
                where.and(item.getDateTime("publishedAt", java.time.Instant.class).isNull());
            }
        }
        if (classification != null && !classification.isBlank()) {
            where.and(cls.getString("name").equalsIgnoreCase(classification));
        }

        JPAQuery<ItemEntity> baseQuery = queryFactory
            .select(item)
            .from(item)
            .leftJoin(icm).on(icm.get("item").get("id", UUID.class).eq(item.get("id", UUID.class)))
            .leftJoin(cls).on(icm.get("classification").get("id", UUID.class).eq(cls.get("id", UUID.class)))
            .where(where)
            .distinct();

        List<ItemEntity> content = baseQuery
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

        Long total = queryFactory
            .select(Expressions.numberTemplate(Long.class, "count(distinct {0})", item.get("id")))
            .from(item)
            .leftJoin(icm).on(icm.get("item").get("id", UUID.class).eq(item.get("id", UUID.class)))
            .leftJoin(cls).on(icm.get("classification").get("id", UUID.class).eq(cls.get("id", UUID.class)))
            .where(where)
            .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0L : total);
    }
}
