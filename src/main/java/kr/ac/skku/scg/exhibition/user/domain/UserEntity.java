package kr.ac.skku.scg.exhibition.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import kr.ac.skku.scg.exhibition.global.entity.BaseEntity;

@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_email", columnNames = {"email"}),
                @UniqueConstraint(name = "uk_users_ci", columnNames = {"ci"})
        }
)
public class UserEntity extends BaseEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 200)
    private String ci;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 200)
    private String email;

    @Column(nullable = false, length = 20)
    private String role = "visitor";

    @Column
    private Instant lastLoginAt;

    protected UserEntity() {
    }

    public UserEntity(UUID id, String ci, String name, String email, String role) {
        this.id = id;
        this.ci = ci;
        this.name = name;
        this.email = email;
        this.role = role;
    }

    @PrePersist
    void ensureRoleOnCreate() {
        if (role == null || role.isBlank()) {
            role = "visitor";
        }
    }

    @PreUpdate
    void ensureRoleOnUpdate() {
        if (role == null || role.isBlank()) {
            role = "visitor";
        }
    }

    public UUID getId() {
        return id;
    }

    public String getCi() {
        return ci;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }
}
