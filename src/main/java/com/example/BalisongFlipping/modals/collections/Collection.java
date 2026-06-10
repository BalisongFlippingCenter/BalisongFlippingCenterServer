package com.example.BalisongFlipping.modals.collections;

import jakarta.persistence.*;

@Entity
@Table(name = "collections")
public class Collection {

    public Collection() {}

    public Collection(Long userId) {
        this.userId = userId;
        this.bannerImg = "";
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private Long userId;

    private String bannerImg;

    private Long featuredKnife;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getBannerImg() { return bannerImg; }
    public void setBannerImg(String bannerImg) { this.bannerImg = bannerImg; }

    public Long getFeaturedKnife() { return featuredKnife; }
    public void setFeaturedKnife(Long featuredKnife) { this.featuredKnife = featuredKnife; }
}
