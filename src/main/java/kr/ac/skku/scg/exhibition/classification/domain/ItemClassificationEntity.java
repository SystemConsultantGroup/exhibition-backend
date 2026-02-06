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
import kr.ac.skku.scg.exhibition.exhibition.domain.ExhibitionServiceEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "item_classifications",
    indexes = @Index(name = "idx_classification_exhibition", columnList = "exhibition_id"),
    uniqueConstraints = @UniqueConstraint(name = "uk_classification_exhibition_name", columnNames = {"exhibition_id", "name"})
)
public class ItemClassificationEntity {

    @Id
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exhibition_id", nullable = false)
    private ExhibitionServiceEntity exhibition;

    @Column(length = 100, nullable = false)
    private String name;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
