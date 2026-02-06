package kr.ac.skku.scg.exhibition.category.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import kr.ac.skku.scg.exhibition.exhibition.domain.ExhibitionEntity;
import kr.ac.skku.scg.exhibition.global.entity.BaseEntity;

@Entity
@Table(name = "categories")
public class CategoryEntity extends BaseEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exhibition_id")
    private ExhibitionEntity exhibition;

    @Column(nullable = false, length = 128)
    private String name;

    protected CategoryEntity() {
    }

    public CategoryEntity(UUID id, ExhibitionEntity exhibition, String name) {
        this.id = id;
        this.exhibition = exhibition;
        this.name = name;
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

}
