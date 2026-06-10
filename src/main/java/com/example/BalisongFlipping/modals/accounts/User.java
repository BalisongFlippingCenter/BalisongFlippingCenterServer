package com.example.BalisongFlipping.modals.accounts;

import jakarta.persistence.*;

@Entity
@DiscriminatorValue("USER")
public class User extends Account {

    public User() {}

    public User(String email, String password, String displayName, String identifierCode) {
        super(email, password);
        this.displayName = displayName;
        this.identifierCode = identifierCode;
        this.profileImg = "";
        this.bannerImg = "";
        this.collectionId = null;
        setRole("USER");
        this.bio = "";
        this.measurementUnit = "IMPERIAL";
        this.currency = "USD";
        this.isHidden = false;
        this.facebookLink = "";
        this.twitterLink = "";
        this.instagramLink = "";
        this.youtubeLink = "";
        this.discordLink = "";
        this.redditLink = "";
        this.personalEmailLink = "";
        this.personalWebsiteLink = "";
    }

    private String displayName;
    private String identifierCode;
    private String profileImg;
    private String bannerImg;
    private Long collectionId;

    @jakarta.persistence.Column(length = 150)
    private String bio;
    private String measurementUnit;
    private String currency;
    private boolean isHidden;

    private String facebookLink;
    private String twitterLink;
    private String instagramLink;
    private String youtubeLink;
    private String discordLink;
    private String redditLink;
    private String personalEmailLink;
    private String personalWebsiteLink;

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getIdentifierCode() { return identifierCode; }
    public void setIdentifierCode(String identifierCode) { this.identifierCode = identifierCode; }

    public String getProfileImg() { return profileImg; }
    public void setProfileImg(String profileImg) { this.profileImg = profileImg; }

    public String getBannerImg() { return bannerImg; }
    public void setBannerImg(String bannerImg) { this.bannerImg = bannerImg; }

    public Long getCollectionId() { return collectionId; }
    public void setCollectionId(Long collectionId) { this.collectionId = collectionId; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getMeasurementUnit() { return measurementUnit; }
    public void setMeasurementUnit(String measurementUnit) { this.measurementUnit = measurementUnit; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public boolean isHidden() { return isHidden; }
    public void setHidden(boolean isHidden) { this.isHidden = isHidden; }

    public String getFacebookLink() { return facebookLink; }
    public void setFacebookLink(String facebookLink) { this.facebookLink = facebookLink; }

    public String getTwitterLink() { return twitterLink; }
    public void setTwitterLink(String twitterLink) { this.twitterLink = twitterLink; }

    public String getInstagramLink() { return instagramLink; }
    public void setInstagramLink(String instagramLink) { this.instagramLink = instagramLink; }

    public String getYoutubeLink() { return youtubeLink; }
    public void setYoutubeLink(String youtubeLink) { this.youtubeLink = youtubeLink; }

    public String getDiscordLink() { return discordLink; }
    public void setDiscordLink(String discordLink) { this.discordLink = discordLink; }

    public String getRedditLink() { return redditLink; }
    public void setRedditLink(String redditLink) { this.redditLink = redditLink; }

    public String getPersonalEmailLink() { return personalEmailLink; }
    public void setPersonalEmailLink(String personalEmailLink) { this.personalEmailLink = personalEmailLink; }

    public String getPersonalWebsiteLink() { return personalWebsiteLink; }
    public void setPersonalWebsiteLink(String personalWebsiteLink) { this.personalWebsiteLink = personalWebsiteLink; }
}
