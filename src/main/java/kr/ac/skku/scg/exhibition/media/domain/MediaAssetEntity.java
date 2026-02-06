package kr.ac.skku.scg.exhibition.media.domain;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import kr.ac.skku.scg.exhibition.exhibition.domain.ExhibitionServiceEntity;
import kr.ac.skku.scg.exhibition.item.domain.ItemEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "media_assets",
    indexes = {
        @Index(name = "idx_media_exhibition_item", columnList = "exhibition_id,item_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_media_exhibition_object_key", columnNames = {"exhibition_id", "object_key"})
    }
)
public class MediaAssetEntity {

    @Id
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private ItemEntity item;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exhibition_id", nullable = false)
    private ExhibitionServiceEntity exhibition;

    @Column(length = 255, nullable = false)
    private String objectKey;

    @Column(length = 50, nullable = false)
    private String mediaType;

    @Column(nullable = false)
    private long size;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
