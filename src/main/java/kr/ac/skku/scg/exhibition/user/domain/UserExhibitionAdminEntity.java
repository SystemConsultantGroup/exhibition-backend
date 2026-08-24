package kr.ac.skku.scg.exhibition.user.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import kr.ac.skku.scg.exhibition.exhibition.domain.ExhibitionEntity;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(
        name = "user_exhibition_admin",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_exhibition_admin_user_exhibition",
                columnNames = {"user_id", "exhibition_id"}
        )
)
public class UserExhibitionAdminEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exhibition_id", nullable = false)
    private ExhibitionEntity exhibition;

    protected UserExhibitionAdminEntity() {
    }

    public UserExhibitionAdminEntity(UUID id, UserEntity user, ExhibitionEntity exhibition) {
        this.id = id;
        this.user = user;
        this.exhibition = exhibition;
    }

    public UUID getId() {
        return id;
    }

    public UserEntity getUser() {
        return user;
    }

    public ExhibitionEntity getExhibition() {
        return exhibition;
    }
}
