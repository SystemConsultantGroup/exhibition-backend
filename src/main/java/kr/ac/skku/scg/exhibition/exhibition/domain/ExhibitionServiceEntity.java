package kr.ac.skku.scg.exhibition.exhibition.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import kr.ac.skku.scg.exhibition.media.domain.MediaAssetEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "exhibition_services")
public class ExhibitionServiceEntity {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(length = 64, nullable = false, unique = true)
    private String slug;

    @Column(length = 128, nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private LocalDate startDate;

    private LocalDate endDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "logo_media_id")
    private MediaAssetEntity logoMedia;

    @Column(nullable = false)
    private boolean isActive = true;

    @Column(nullable = false)
    private boolean popupEnabled = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "popup_image_media_id")
    private MediaAssetEntity popupImageMedia;

    @Column(length = 200)
    private String introTitle;

    @Column(columnDefinition = "TEXT")
    private String introDescription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "intro_video_media_id")
    private MediaAssetEntity introVideoMedia;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;
}
