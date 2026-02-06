package kr.ac.skku.scg.exhibition.item.domain;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import kr.ac.skku.scg.exhibition.category.domain.CategoryEntity;
import kr.ac.skku.scg.exhibition.exhibition.domain.ExhibitionServiceEntity;
import kr.ac.skku.scg.exhibition.media.domain.MediaAssetEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "items",
    indexes = {
        @Index(name = "idx_item_exhibition_category", columnList = "exhibition_id,category_id"),
        @Index(name = "idx_item_exhibition_published", columnList = "exhibition_id,published_at"),
        @Index(name = "idx_item_exhibition_title", columnList = "exhibition_id,title")
    }
)
public class ItemEntity {

    @Id
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exhibition_id", nullable = false)
    private ExhibitionServiceEntity exhibition;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private CategoryEntity category;

    @Column(length = 200, nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 100)
    private String authorName;

    @Column(length = 200)
    private String authorEmail;

    @Column(columnDefinition = "TEXT")
    private String participantNames;

    @Column(columnDefinition = "TEXT")
    private String advisorNames;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thumbnail_media_id")
    private MediaAssetEntity thumbnailMedia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poster_media_id")
    private MediaAssetEntity posterMedia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "presentation_video_media_id")
    private MediaAssetEntity presentationVideoMedia;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private ItemVisibility visibility = ItemVisibility.PUBLIC;

    private Instant publishedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;
}
