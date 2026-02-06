package kr.ac.skku.scg.exhibition.classification.domain;

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
import kr.ac.skku.scg.exhibition.item.domain.ItemEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "item_classification_map",
    indexes = {
        @Index(name = "idx_icm_item", columnList = "item_id"),
        @Index(name = "idx_icm_classification", columnList = "classification_id")
    },
    uniqueConstraints = @UniqueConstraint(name = "uk_icm_item_classification", columnNames = {"item_id", "classification_id"})
)
public class ItemClassificationMapEntity {

    @Id
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private ItemEntity item;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "classification_id", nullable = false)
    private ItemClassificationEntity classification;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
