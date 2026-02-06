package kr.ac.skku.scg.exhibition.classification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import kr.ac.skku.scg.exhibition.exhibition.domain.ExhibitionEntity;

@Entity
@Table(name = "item_classifications")
public class ItemClassificationEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exhibition_id")
    private ExhibitionEntity exhibition;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private Instant createdAt;

    protected ItemClassificationEntity() {
    }

    public ItemClassificationEntity(UUID id, ExhibitionEntity exhibition, String name) {
        this.id = id;
        this.exhibition = exhibition;
        this.name = name;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public ExhibitionEntity getExhibition() {
        return exhibition;
    }

    public String getName() {
        return name;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
