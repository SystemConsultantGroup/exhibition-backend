package kr.ac.skku.scg.exhibition.category.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.text.Normalizer;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;
import kr.ac.skku.scg.exhibition.exhibition.domain.ExhibitionEntity;
import kr.ac.skku.scg.exhibition.global.entity.BaseEntity;

@Entity
@Table(name = "categories", uniqueConstraints = {
        @UniqueConstraint(name = "uk_categories_exhibition_name", columnNames = {"exhibition_id", "name"}),
        @UniqueConstraint(name = "uk_categories_exhibition_slug", columnNames = {"exhibition_id", "slug"})
})
public class CategoryEntity extends BaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exhibition_id")
    private ExhibitionEntity exhibition;

    @Column(nullable = false, length = 160)
    private String slug;

    @Column(nullable = false, length = 128)
    private String name;

    protected CategoryEntity() {
    }

    public CategoryEntity(UUID id, ExhibitionEntity exhibition, String name) {
        this(id, exhibition, createSlug(name), name);
    }

    public CategoryEntity(UUID id, ExhibitionEntity exhibition, String slug, String name) {
        this.id = id;
        this.exhibition = exhibition;
        this.slug = slug;
        this.name = name;
    }

    public UUID getId() {
        return id;
    }

    public ExhibitionEntity getExhibition() {
        return exhibition;
    }

    public String getSlug() {
        return slug;
    }

    public String getName() {
        return name;
    }

    /** 이름과 파생 슬러그를 함께 수정한다. */
    public void updateName(String name) {
        this.name = name;
        this.slug = createSlug(name);
    }

    /** createdAt 기반 노출 순서를 수정한다. */
    public void reorderAt(Instant createdAt) {
        rewriteCreatedAt(createdAt);
    }

    private static String createSlug(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Category name is required");
        }

        String normalized = Normalizer.normalize(name, Normalizer.Form.NFKC)
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[\\s_]+", "-")
                .replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}-]", "")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-|-$", "");

        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Category slug cannot be blank");
        }
        return normalized;
    }

}
