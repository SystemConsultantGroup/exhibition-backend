package kr.ac.skku.scg.exhibition.category.domain;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
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
import jakarta.persistence.Version;
import kr.ac.skku.scg.exhibition.exhibition.domain.ExhibitionServiceEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "categories",
    indexes = {
        @Index(name = "idx_category_exhibition_parent", columnList = "exhibition_id,parent_id"),
        @Index(name = "idx_category_exhibition_path", columnList = "exhibition_id,path")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_category_sibling_name", columnNames = {"exhibition_id", "parent_id", "name"})
    }
)
public class CategoryEntity {

    @Id
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exhibition_id", nullable = false)
    private ExhibitionServiceEntity exhibition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private CategoryEntity parent;

    @Column(length = 128, nullable = false)
    private String name;

    @Column(nullable = false)
    private int orderIndex = 0;

    @Column(length = 512, nullable = false)
    private String path;

    @Column(nullable = false)
    private int depth = 0;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;
}
