package kr.ac.skku.scg.exhibition.classification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import kr.ac.skku.scg.exhibition.item.domain.ItemEntity;

@Entity
@Table(
        name = "item_classification_map",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_item_classification_map_item_classification", columnNames = {"item_id", "classification_id"})
        }
)
public class ItemClassificationMapEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private ItemEntity item;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "classification_id", nullable = false)
    private ItemClassificationEntity classification;

    @Column(nullable = false)
    private Instant createdAt;

    protected ItemClassificationMapEntity() {
    }

    public ItemClassificationMapEntity(UUID id, ItemEntity item, ItemClassificationEntity classification) {
        this.id = id;
        this.item = item;
        this.classification = classification;
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

    public ItemClassificationEntity getClassification() {
        return classification;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
