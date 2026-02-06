package kr.ac.skku.scg.exhibition.exhibition.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import kr.ac.skku.scg.exhibition.global.entity.BaseEntity;
import kr.ac.skku.scg.exhibition.media.domain.MediaAssetEntity;

@Entity
@Table(name = "exhibition_services")
public class ExhibitionEntity extends BaseEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 64)
    private String slug;

    @Column(nullable = false, unique = true, length = 128)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "logo_media_id")
    private MediaAssetEntity logoMedia;

    @Column(nullable = false)
    private boolean popupEnabled = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "popup_image_media_id")
    private MediaAssetEntity popupImageMedia;

    @Column(columnDefinition = "TEXT")
    private String popupUrl;

    @Column(length = 200)
    private String introTitle;

    @Column(columnDefinition = "TEXT")
    private String introDescription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "intro_video_media_id")
    private MediaAssetEntity introVideoMedia;

    protected ExhibitionEntity() {
    }

    public ExhibitionEntity(UUID id, String slug, String name) {
        this.id = id;
        this.slug = slug;
        this.name = name;
    }

    public UUID getId() {
        return id;
    }

    public String getSlug() {
        return slug;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public MediaAssetEntity getLogoMedia() {
        return logoMedia;
    }

    public boolean isPopupEnabled() {
        return popupEnabled;
    }

    public MediaAssetEntity getPopupImageMedia() {
        return popupImageMedia;
    }

    public String getPopupUrl() {
        return popupUrl;
    }

    public String getIntroTitle() {
        return introTitle;
    }

    public String getIntroDescription() {
        return introDescription;
    }

    public MediaAssetEntity getIntroVideoMedia() {
        return introVideoMedia;
    }

}
