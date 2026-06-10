package com.example.BalisongFlipping.modals.posts;

import jakarta.persistence.*;

@Entity
@DiscriminatorValue("TRADE")
public class TradePost extends PostWrapper {

    // The knife from the user's collection being offered in the trade
    @Column(name = "offering_knife_id")
    private Long offeringKnifeId;

    // Text description of what the user is looking for in return
    @Column(name = "looking_for_text", columnDefinition = "TEXT")
    private String lookingForText;

    public TradePost() {
        super();
    }

    public Long getOfferingKnifeId() { return offeringKnifeId; }
    public void setOfferingKnifeId(Long offeringKnifeId) { this.offeringKnifeId = offeringKnifeId; }

    public String getLookingForText() { return lookingForText; }
    public void setLookingForText(String lookingForText) { this.lookingForText = lookingForText; }
}
