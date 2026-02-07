package kr.ac.skku.scg.exhibition.item.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;
import kr.ac.skku.scg.exhibition.category.domain.CategoryEntity;
import kr.ac.skku.scg.exhibition.eventperiod.domain.EventPeriodEntity;
import kr.ac.skku.scg.exhibition.exhibition.domain.ExhibitionEntity;
import kr.ac.skku.scg.exhibition.global.entity.BaseEntity;
import kr.ac.skku.scg.exhibition.media.domain.MediaAssetEntity;

@Entity
@Table(name = "items")
public class ItemEntity extends BaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
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

    @Column(columnDefinition = "TEXT")
    private String description;

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

    public String getParticipantNames() {
        return participantNames;
    }

    public String getAdvisorNames() {
        return advisorNames;
    }

    public MediaAssetEntity getThumbnailMedia() {
        return thumbnailMedia;
    }

    public MediaAssetEntity getPosterMedia() {
        return posterMedia;
    }

    public MediaAssetEntity getPresentationVideoMedia() {
        return presentationVideoMedia;
    }

}
