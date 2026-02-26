package kr.ac.skku.scg.exhibition.item.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import kr.ac.skku.scg.exhibition.classification.domain.QItemClassificationMapEntity;
import kr.ac.skku.scg.exhibition.item.domain.ItemEntity;
import kr.ac.skku.scg.exhibition.item.domain.QItemEntity;
import kr.ac.skku.scg.exhibition.item.dto.request.ItemListRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class ItemRepositoryImpl implements ItemRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public ItemRepositoryImpl(EntityManager entityManager) {
        this.queryFactory = new JPAQueryFactory(entityManager);
    }

    @Override
    public Page<ItemEntity> search(ItemListRequest request, Pageable pageable) {
        QItemEntity item = QItemEntity.itemEntity;
        QItemClassificationMapEntity itemClassificationMap = QItemClassificationMapEntity.itemClassificationMapEntity;

        BooleanBuilder where = new BooleanBuilder()
                .and(item.exhibition.id.eq(request.getExhibitionId()));

        if (request.getCategoryId() != null) {
            where.and(item.category.id.eq(request.getCategoryId()));
        }
        if (request.getEventPeriodId() != null) {
            where.and(item.eventPeriod.id.eq(request.getEventPeriodId()));
        }
        if (request.getClassificationId() != null) {
            where.and(JPAExpressions.selectOne()
                    .from(itemClassificationMap)
                    .where(itemClassificationMap.item.id.eq(item.id)
                            .and(itemClassificationMap.classification.id.eq(request.getClassificationId())))
                    .exists());
        }

        String q = normalize(request.getQ());
        if (q != null) {
            where.and(item.title.containsIgnoreCase(q)
                    .or(item.advisorNames.containsIgnoreCase(q))
                    .or(item.participantNames.containsIgnoreCase(q)));
        }

        List<ItemEntity> content = queryFactory
                .selectFrom(item)
                .where(where)
                .orderBy(orderSpecifiers(item, pageable))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(item.count())
                .from(item)
                .where(where)
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }

    private OrderSpecifier<?>[] orderSpecifiers(QItemEntity item, Pageable pageable) {
        List<OrderSpecifier<?>> specifiers = new ArrayList<>();
        PathBuilder<ItemEntity> pathBuilder = new PathBuilder<>(ItemEntity.class, item.getMetadata());
        if (pageable.getSort().isSorted()) {
            for (Sort.Order sortOrder : pageable.getSort()) {
                String property = sortOrder.getProperty();
                Order order = sortOrder.isAscending() ? Order.ASC : Order.DESC;
                specifiers.add(new OrderSpecifier<>(order, pathBuilder.getComparable(property, Comparable.class)));
            }
        }

        if (specifiers.isEmpty()) {
            specifiers.add(new OrderSpecifier<>(Order.DESC, item.createdAt));
        }

        specifiers.add(item.id.desc());
        return specifiers.toArray(new OrderSpecifier<?>[0]);
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
