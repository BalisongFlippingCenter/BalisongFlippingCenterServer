package com.example.BalisongFlipping.modals.knifeCatalog;

import com.example.BalisongFlipping.enums.knives.*;
import jakarta.persistence.*;

@Entity
@Table(name = "knife_variants")
public class KnifeVariant {

    public KnifeVariant() {}

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "knife_version_id", nullable = false)
    private KnifeVersion knifeVersion;

    @Column(nullable = false)
    private String variantSlug;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private KnifeType type;

    @Column(nullable = false)
    private String label;

    private Double msrp;

    @Enumerated(EnumType.STRING)
    private BladeStyle bladeStyle;

    @Enumerated(EnumType.STRING)
    private BladeMaterial bladeMaterial;

    @Enumerated(EnumType.STRING)
    private BladeFinish bladeFinish;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public KnifeVersion getKnifeVersion() { return knifeVersion; }
    public void setKnifeVersion(KnifeVersion knifeVersion) { this.knifeVersion = knifeVersion; }

    public String getVariantSlug() { return variantSlug; }
    public void setVariantSlug(String variantSlug) { this.variantSlug = variantSlug; }

    public KnifeType getType() { return type; }
    public void setType(KnifeType type) { this.type = type; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public Double getMsrp() { return msrp; }
    public void setMsrp(Double msrp) { this.msrp = msrp; }

    public BladeStyle getBladeStyle() { return bladeStyle; }
    public void setBladeStyle(BladeStyle bladeStyle) { this.bladeStyle = bladeStyle; }

    public BladeMaterial getBladeMaterial() { return bladeMaterial; }
    public void setBladeMaterial(BladeMaterial bladeMaterial) { this.bladeMaterial = bladeMaterial; }

    public BladeFinish getBladeFinish() { return bladeFinish; }
    public void setBladeFinish(BladeFinish bladeFinish) { this.bladeFinish = bladeFinish; }
}
