package kr.ac.skku.scg.exhibition.exhibition.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
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

    @Column(length = 255)
    private String logoObjectKey;

    @Column(nullable = false)
    private boolean isActive = true;

    @Column(nullable = false)
    private boolean popupEnabled = false;

    @Column(length = 255)
    private String popupImageUrl;

    @Column(length = 255)
    private String introVideoUrl;

    @Column(length = 200)
    private String introTitle;

    @Column(columnDefinition = "TEXT")
    private String introDescription;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;
}
