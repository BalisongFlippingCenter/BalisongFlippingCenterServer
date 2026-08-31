package com.example.BalisongFlipping.modals.knifeCatalog;

import com.example.BalisongFlipping.enums.knives.SourceType;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "where_to_find")
public class WhereToFind {

    public WhereToFind() {}

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "knife_version_id", nullable = false)
    private KnifeVersion knifeVersion;

    @Column(nullable = false)
    private String label;

    @Column(columnDefinition = "TEXT")
    private String url;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SourceType type;

    @Column(columnDefinition = "TEXT")
    private String note;

    private int sortOrder;

    private Instant lastCheckedAt;

    private String contentHash;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public KnifeVersion getKnifeVersion() { return knifeVersion; }
    public void setKnifeVersion(KnifeVersion knifeVersion) { this.knifeVersion = knifeVersion; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public SourceType getType() { return type; }
    public void setType(SourceType type) { this.type = type; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }

    public Instant getLastCheckedAt() { return lastCheckedAt; }
    public void setLastCheckedAt(Instant lastCheckedAt) { this.lastCheckedAt = lastCheckedAt; }

    public String getContentHash() { return contentHash; }
    public void setContentHash(String contentHash) { this.contentHash = contentHash; }
}
