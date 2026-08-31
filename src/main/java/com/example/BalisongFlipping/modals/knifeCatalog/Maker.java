package com.example.BalisongFlipping.modals.knifeCatalog;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "makers")
public class Maker {

    public Maker() {}

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    private String name;

    private String country;

    @Column(columnDefinition = "TEXT")
    private String knownFor;

    @Column(columnDefinition = "TEXT")
    private String officialSiteUrl;

    private Instant lastCheckedAt;

    private String contentHash;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getKnownFor() { return knownFor; }
    public void setKnownFor(String knownFor) { this.knownFor = knownFor; }

    public String getOfficialSiteUrl() { return officialSiteUrl; }
    public void setOfficialSiteUrl(String officialSiteUrl) { this.officialSiteUrl = officialSiteUrl; }

    public Instant getLastCheckedAt() { return lastCheckedAt; }
    public void setLastCheckedAt(Instant lastCheckedAt) { this.lastCheckedAt = lastCheckedAt; }

    public String getContentHash() { return contentHash; }
    public void setContentHash(String contentHash) { this.contentHash = contentHash; }
}
