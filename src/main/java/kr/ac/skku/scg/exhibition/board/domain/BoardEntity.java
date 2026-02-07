package kr.ac.skku.scg.exhibition.board.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;
import kr.ac.skku.scg.exhibition.exhibition.domain.ExhibitionEntity;
import kr.ac.skku.scg.exhibition.global.entity.BaseEntity;
import kr.ac.skku.scg.exhibition.media.domain.MediaAssetEntity;
import kr.ac.skku.scg.exhibition.user.domain.UserEntity;

@Entity
@Table(name = "boards")
public class BoardEntity extends BaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exhibition_id", nullable = false)
    private ExhibitionEntity exhibition;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @OneToMany(mappedBy = "board", fetch = FetchType.LAZY)
    private List<MediaAssetEntity> attachmentMediaList = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_user_id", nullable = false)
    private UserEntity authorUser;

    protected BoardEntity() {
    }

    public BoardEntity(
            UUID id,
            ExhibitionEntity exhibition,
            String title,
            String content,
            List<MediaAssetEntity> attachmentMediaList,
            UserEntity authorUser
    ) {
        this.id = id;
        this.exhibition = exhibition;
        this.title = title;
        this.content = content;
        if (attachmentMediaList != null) {
            this.attachmentMediaList = attachmentMediaList;
        }
        this.authorUser = authorUser;
    }

    public UUID getId() {
        return id;
    }

    public ExhibitionEntity getExhibition() {
        return exhibition;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public List<MediaAssetEntity> getAttachmentMediaList() {
        return attachmentMediaList;
    }

    public UserEntity getAuthorUser() {
        return authorUser;
    }

}
