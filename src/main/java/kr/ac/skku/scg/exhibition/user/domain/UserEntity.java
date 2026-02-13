package kr.ac.skku.scg.exhibition.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;
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
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(nullable = false, length = 200)
    private String ci;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = true, length = 200)
    private String email;

    @Column(nullable = true, length = 100)
    private String department;

    @Column(nullable = true, length = 30)
    private String phoneNumber;

    @Column(nullable = true, length = 30)
    private String studentNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserType role = UserType.VISITOR;

    @Column
    private Instant lastLoginAt;

    protected UserEntity() {
    }

    public UserEntity(UUID id, String ci, String name, String email, UserType role) {
        this.id = id;
        this.ci = ci;
        this.name = name;
        this.email = email;
        this.role = role;
    }

    public UserEntity(UUID id, String ci, String name, String email, String department, String phoneNumber,
            String studentNumber, UserType role) {
        this.id = id;
        this.ci = ci;
        this.name = name;
        this.email = email;
        this.department = department;
        this.phoneNumber = phoneNumber;
        this.studentNumber = studentNumber;
        this.role = role;
    }

    @PrePersist
    void ensureRoleOnCreate() {
        if (role == null) {
            role = UserType.VISITOR;
        }
    }

    @PreUpdate
    void ensureRoleOnUpdate() {
        if (role == null) {
            role = UserType.VISITOR;
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

    public String getDepartment() {
        return department;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getStudentNumber() {
        return studentNumber;
    }

    public UserType getRole() {
        return role;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }
}
