package kr.ac.skku.scg.exhibition.item.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import kr.ac.skku.scg.exhibition.category.domain.CategoryEntity;
import kr.ac.skku.scg.exhibition.eventperiod.domain.EventPeriodEntity;
import kr.ac.skku.scg.exhibition.exhibition.domain.ExhibitionEntity;

@Entity
@Table(name = "items")
public class ItemEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exhibition_id")
    private ExhibitionEntity exhibition;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id")
    private CategoryEntity category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_period_id")
    private EventPeriodEntity eventPeriod;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected ItemEntity() {
    }

    public ItemEntity(UUID id, ExhibitionEntity exhibition, CategoryEntity category, EventPeriodEntity eventPeriod,
                      String title, String description) {
        this.id = id;
        this.exhibition = exhibition;
        this.category = category;
        this.eventPeriod = eventPeriod;
        this.title = title;
        this.description = description;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public ExhibitionEntity getExhibition() {
        return exhibition;
    }

    public CategoryEntity getCategory() {
        return category;
    }

    public EventPeriodEntity getEventPeriod() {
        return eventPeriod;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
