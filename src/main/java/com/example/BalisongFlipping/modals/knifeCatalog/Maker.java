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

    @Column(columnDefinition = "TEXT")
    private String logoUrl;

    private Integer foundedYear;

    @Column(columnDefinition = "TEXT")
    private String instagramUrl;

    @Column(columnDefinition = "TEXT")
    private String youtubeUrl;

    @Column(columnDefinition = "TEXT")
    private String facebookUrl;

    @Column(columnDefinition = "TEXT")
    private String twitterUrl;

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

    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }

    public Integer getFoundedYear() { return foundedYear; }
    public void setFoundedYear(Integer foundedYear) { this.foundedYear = foundedYear; }

    public String getInstagramUrl() { return instagramUrl; }
    public void setInstagramUrl(String instagramUrl) { this.instagramUrl = instagramUrl; }

    public String getYoutubeUrl() { return youtubeUrl; }
    public void setYoutubeUrl(String youtubeUrl) { this.youtubeUrl = youtubeUrl; }

    public String getFacebookUrl() { return facebookUrl; }
    public void setFacebookUrl(String facebookUrl) { this.facebookUrl = facebookUrl; }

    public String getTwitterUrl() { return twitterUrl; }
    public void setTwitterUrl(String twitterUrl) { this.twitterUrl = twitterUrl; }

    public Instant getLastCheckedAt() { return lastCheckedAt; }
    public void setLastCheckedAt(Instant lastCheckedAt) { this.lastCheckedAt = lastCheckedAt; }

    public String getContentHash() { return contentHash; }
    public void setContentHash(String contentHash) { this.contentHash = contentHash; }
}
