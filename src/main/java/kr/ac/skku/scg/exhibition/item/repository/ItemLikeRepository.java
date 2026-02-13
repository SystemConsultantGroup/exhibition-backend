package kr.ac.skku.scg.exhibition.item.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import kr.ac.skku.scg.exhibition.item.domain.ItemLikeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ItemLikeRepository extends JpaRepository<ItemLikeEntity, UUID> {

    long countByItem_Id(UUID itemId);

    @Query("""
            select il.item.id as itemId, count(il) as likeCount
            from ItemLikeEntity il
            where il.item.id in :itemIds
            group by il.item.id
            """)
    List<ItemLikeCount> countLikesByItemIds(@Param("itemIds") Collection<UUID> itemIds);

    interface ItemLikeCount {
        UUID getItemId();

        long getLikeCount();
    }
}
