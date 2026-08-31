package com.example.BalisongFlipping.modals.knifeCatalog;

import com.example.BalisongFlipping.enums.knives.*;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "knife_versions")
public class KnifeVersion {

    public KnifeVersion() {}

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "knife_id", nullable = false)
    private Knife knife;

    @Column(nullable = false)
    private String versionSlug;

    @Column(nullable = false)
    private String versionLabel;

    private boolean discontinued;

    private Integer releaseYear;

    @Column(columnDefinition = "TEXT")
    private String description;

    private Double overallLength;
    private Double weight;

    @Enumerated(EnumType.STRING)
    private PivotSystem pivotSystem;

    @Enumerated(EnumType.STRING)
    private LatchType latchType;

    @Enumerated(EnumType.STRING)
    private PinSystem pinSystem;

    private boolean hasModularBalance;
    private String balanceValue;

    @Enumerated(EnumType.STRING)
    private HandleConstruction handleConstruction;

    @Enumerated(EnumType.STRING)
    private HandleMaterial handleMaterial;

    @Enumerated(EnumType.STRING)
    private HandleFinish handleFinish;

    @OneToMany(mappedBy = "knifeVersion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<KnifeVariant> variants = new ArrayList<>();

    @OneToMany(mappedBy = "knifeVersion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WhereToFind> whereToFind = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Knife getKnife() { return knife; }
    public void setKnife(Knife knife) { this.knife = knife; }

    public String getVersionSlug() { return versionSlug; }
    public void setVersionSlug(String versionSlug) { this.versionSlug = versionSlug; }

    public String getVersionLabel() { return versionLabel; }
    public void setVersionLabel(String versionLabel) { this.versionLabel = versionLabel; }

    public boolean isDiscontinued() { return discontinued; }
    public void setDiscontinued(boolean discontinued) { this.discontinued = discontinued; }

    public Integer getReleaseYear() { return releaseYear; }
    public void setReleaseYear(Integer releaseYear) { this.releaseYear = releaseYear; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Double getOverallLength() { return overallLength; }
    public void setOverallLength(Double overallLength) { this.overallLength = overallLength; }

    public Double getWeight() { return weight; }
    public void setWeight(Double weight) { this.weight = weight; }

    public PivotSystem getPivotSystem() { return pivotSystem; }
    public void setPivotSystem(PivotSystem pivotSystem) { this.pivotSystem = pivotSystem; }

    public LatchType getLatchType() { return latchType; }
    public void setLatchType(LatchType latchType) { this.latchType = latchType; }

    public PinSystem getPinSystem() { return pinSystem; }
    public void setPinSystem(PinSystem pinSystem) { this.pinSystem = pinSystem; }

    public boolean isHasModularBalance() { return hasModularBalance; }
    public void setHasModularBalance(boolean hasModularBalance) { this.hasModularBalance = hasModularBalance; }

    public String getBalanceValue() { return balanceValue; }
    public void setBalanceValue(String balanceValue) { this.balanceValue = balanceValue; }

    public HandleConstruction getHandleConstruction() { return handleConstruction; }
    public void setHandleConstruction(HandleConstruction handleConstruction) { this.handleConstruction = handleConstruction; }

    public HandleMaterial getHandleMaterial() { return handleMaterial; }
    public void setHandleMaterial(HandleMaterial handleMaterial) { this.handleMaterial = handleMaterial; }

    public HandleFinish getHandleFinish() { return handleFinish; }
    public void setHandleFinish(HandleFinish handleFinish) { this.handleFinish = handleFinish; }

    public List<KnifeVariant> getVariants() { return variants; }
    public void setVariants(List<KnifeVariant> variants) { this.variants = variants; }

    public List<WhereToFind> getWhereToFind() { return whereToFind; }
    public void setWhereToFind(List<WhereToFind> whereToFind) { this.whereToFind = whereToFind; }
}
