package com.example.BalisongFlipping.modals.posts;

import com.example.BalisongFlipping.enums.posts.BuySellMode;
import jakarta.persistence.*;

@Entity
@DiscriminatorValue("BUY_SELL")
public class BuySellPost extends PostWrapper {

    @Enumerated(EnumType.STRING)
    @Column(name = "mode")
    private BuySellMode mode;

    // Only populated when mode = SELLING — the knife from the user's collection being sold
    @Column(name = "offering_knife_id")
    private Long offeringKnifeId;

    public BuySellPost() {
        super();
    }

    public BuySellMode getMode() { return mode; }
    public void setMode(BuySellMode mode) { this.mode = mode; }

    public Long getOfferingKnifeId() { return offeringKnifeId; }
    public void setOfferingKnifeId(Long offeringKnifeId) { this.offeringKnifeId = offeringKnifeId; }
}
