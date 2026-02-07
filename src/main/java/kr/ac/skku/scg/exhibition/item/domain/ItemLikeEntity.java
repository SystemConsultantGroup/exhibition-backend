package kr.ac.skku.scg.exhibition.item.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;
import kr.ac.skku.scg.exhibition.user.domain.UserEntity;

@Entity
@Table(
        name = "item_likes",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_item_likes_item_user", columnNames = {"item_id", "user_id"})
        }
)
public class ItemLikeEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private ItemEntity item;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(nullable = false)
    private Instant createdAt;

    protected ItemLikeEntity() {
    }

    public ItemLikeEntity(UUID id, ItemEntity item, UserEntity user) {
        this.id = id;
        this.item = item;
        this.user = user;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public ItemEntity getItem() {
        return item;
    }

    public UserEntity getUser() {
        return user;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
