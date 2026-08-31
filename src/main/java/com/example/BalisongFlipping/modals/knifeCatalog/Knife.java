package com.example.BalisongFlipping.modals.knifeCatalog;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "knives")
public class Knife {

    public Knife() {}

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    private String name;

    @ManyToOne(optional = false)
    @JoinColumn(name = "maker_id", nullable = false)
    private Maker maker;

    @OneToMany(mappedBy = "knife", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<KnifeVersion> versions = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Maker getMaker() { return maker; }
    public void setMaker(Maker maker) { this.maker = maker; }

    public List<KnifeVersion> getVersions() { return versions; }
    public void setVersions(List<KnifeVersion> versions) { this.versions = versions; }
}
