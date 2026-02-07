package kr.ac.skku.scg.exhibition.media.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import kr.ac.skku.scg.exhibition.board.domain.BoardEntity;
import org.hibernate.annotations.UuidGenerator;
import kr.ac.skku.scg.exhibition.exhibition.domain.ExhibitionEntity;
import kr.ac.skku.scg.exhibition.item.domain.ItemEntity;

@Entity
@Table(name = "media_assets")
public class MediaAssetEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private ItemEntity item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id")
    private BoardEntity board;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exhibition_id", nullable = false)
    private ExhibitionEntity exhibition;

    @Column(nullable = false, length = 255)
    private String objectKey;

    @Column(nullable = false, length = 50)
    private String mediaType;

    @Column(nullable = false)
    private long size;

    @Column(nullable = false)
    private Instant createdAt;

    protected MediaAssetEntity() {
    }

    public MediaAssetEntity(UUID id, ItemEntity item, ExhibitionEntity exhibition, String objectKey, String mediaType, long size) {
        this.id = id;
        this.item = item;
        this.exhibition = exhibition;
        this.objectKey = objectKey;
        this.mediaType = mediaType;
        this.size = size;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public ItemEntity getItem() {
        return item;
    }

    public BoardEntity getBoard() {
        return board;
    }

    public ExhibitionEntity getExhibition() {
        return exhibition;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public String getMediaType() {
        return mediaType;
    }

    public long getSize() {
        return size;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
