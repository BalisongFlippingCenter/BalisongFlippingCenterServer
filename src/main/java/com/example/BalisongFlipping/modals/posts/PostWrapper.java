package com.example.BalisongFlipping.modals.posts;

import jakarta.persistence.*;

import java.util.Date;
import java.util.List;

@Entity
@Table(name = "posts")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "post_type", discriminatorType = DiscriminatorType.STRING)
public abstract class PostWrapper {

    public PostWrapper() {}

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String accountId;

    @Column(length = 255)
    private String caption;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "reference_knife_id")
    private Long referenceKnifeId;

    private Date creationDate;

    @ElementCollection
    @CollectionTable(name = "post_media", joinColumns = @JoinColumn(name = "post_id"))
    private List<PostMedia> mediaFiles;

    @Column(name = "like_count")
    private int likeCount = 0;

    @Column(name = "comment_count")
    private int commentCount = 0;

    // Returns the discriminator value as a virtual JSON field so the frontend
    // knows which post type it is receiving without a separate postType field.
    @Transient
    public String getPostType() {
        DiscriminatorValue dv = this.getClass().getAnnotation(DiscriminatorValue.class);
        return dv != null ? dv.value() : "UNKNOWN";
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }

    public String getCaption() { return caption; }
    public void setCaption(String caption) { this.caption = caption; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Long getReferenceKnifeId() { return referenceKnifeId; }
    public void setReferenceKnifeId(Long referenceKnifeId) { this.referenceKnifeId = referenceKnifeId; }

    public Date getCreationDate() { return creationDate; }
    public void setCreationDate(Date creationDate) { this.creationDate = creationDate; }

    public List<PostMedia> getMediaFiles() { return mediaFiles; }
    public void setMediaFiles(List<PostMedia> mediaFiles) { this.mediaFiles = mediaFiles; }

    public int getLikeCount() { return likeCount; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }

    public int getCommentCount() { return commentCount; }
    public void setCommentCount(int commentCount) { this.commentCount = commentCount; }
}
