package com.example.BalisongFlipping.seed;

import com.example.BalisongFlipping.dtos.catalogSeedDtos.*;
import com.example.BalisongFlipping.enums.knives.KnifeType;
import com.example.BalisongFlipping.modals.knifeCatalog.*;
import com.example.BalisongFlipping.repositories.KnifeRepository;
import com.example.BalisongFlipping.repositories.MakerRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.List;

@Service
public class CatalogSeedService {

    private static final Logger log = LoggerFactory.getLogger(CatalogSeedService.class);

    private final MakerRepository makerRepository;
    private final KnifeRepository knifeRepository;
    private final ObjectMapper objectMapper;

    public CatalogSeedService(MakerRepository makerRepository, KnifeRepository knifeRepository, ObjectMapper objectMapper) {
        this.makerRepository = makerRepository;
        this.knifeRepository = knifeRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void seed() throws Exception {
        List<MakerSeedDto> makerDtos = readSeedFile("seed-data/makers.json", new TypeReference<List<MakerSeedDto>>() {});
        List<KnifeSeedDto> knifeDtos = readSeedFile("seed-data/knives.json", new TypeReference<List<KnifeSeedDto>>() {});
        importCatalog(makerDtos, knifeDtos);
    }

    // Shared by the classpath-file seed above (initial/bootstrap load) and
    // the admin-triggered bulk import endpoint (ongoing content updates,
    // no deploy required) -- same upsert-by-slug semantics either way.
    @Transactional
    public void importCatalog(List<MakerSeedDto> makerDtos, List<KnifeSeedDto> knifeDtos) {
        for (MakerSeedDto dto : makerDtos) {
            upsertMaker(dto);
        }

        for (KnifeSeedDto dto : knifeDtos) {
            Maker maker = upsertMaker(new MakerSeedDto(dto.makerSlug(), dto.maker(), null, null, null, null, null, null, null, null, null));
            seedKnife(dto, maker);
        }

        log.info("Catalog import complete: {} makers, {} knives", makerRepository.count(), knifeRepository.count());
    }

    @Transactional
    public Maker createMaker(MakerSeedDto dto) {
        if (makerRepository.findBySlug(dto.slug()).isPresent()) {
            throw new IllegalStateException("A maker with slug '" + dto.slug() + "' already exists");
        }
        return upsertMaker(dto);
    }

    @Transactional
    public Maker updateMaker(String slug, MakerSeedDto dto) {
        makerRepository.findBySlug(slug)
                .orElseThrow(() -> new IllegalArgumentException("No maker found with slug '" + slug + "'"));
        // slug is immutable after creation (used in URLs) -- always apply
        // updates against the path's slug, ignoring whatever the body sent.
        return upsertMaker(new MakerSeedDto(
                slug, dto.name(), dto.country(), dto.knownFor(), dto.officialSiteUrl(), dto.logoUrl(),
                dto.foundedYear(), dto.instagramUrl(), dto.youtubeUrl(), dto.facebookUrl(), dto.twitterUrl()
        ));
    }

    @Transactional
    public void deleteMaker(String slug) {
        Maker maker = makerRepository.findBySlug(slug)
                .orElseThrow(() -> new IllegalArgumentException("No maker found with slug '" + slug + "'"));
        if (!knifeRepository.findByMaker(maker).isEmpty()) {
            throw new IllegalStateException("Cannot delete maker '" + slug + "' -- it still has knives referencing it");
        }
        makerRepository.delete(maker);
    }

    @Transactional
    public Knife createKnife(KnifeSeedDto dto) {
        if (knifeRepository.findBySlug(dto.slug()).isPresent()) {
            throw new IllegalStateException("A knife with slug '" + dto.slug() + "' already exists");
        }
        Maker maker = requireMaker(dto.makerSlug());
        return seedKnife(dto, maker);
    }

    @Transactional
    public Knife updateKnife(String slug, KnifeSeedDto dto) {
        knifeRepository.findBySlug(slug)
                .orElseThrow(() -> new IllegalArgumentException("No knife found with slug '" + slug + "'"));
        Maker maker = requireMaker(dto.makerSlug());
        // slug is immutable after creation (used in URLs) -- always apply
        // updates against the path's slug, ignoring whatever the body sent.
        KnifeSeedDto corrected = new KnifeSeedDto(
                slug, dto.name(), dto.maker(), dto.makerSlug(), dto.bladeStyle(), dto.priceRange(),
                dto.coverPhotoUrl(), dto.description(), dto.versions()
        );
        return seedKnife(corrected, maker);
    }

    @Transactional
    public void deleteKnife(String slug) {
        Knife knife = knifeRepository.findBySlug(slug)
                .orElseThrow(() -> new IllegalArgumentException("No knife found with slug '" + slug + "'"));
        knifeRepository.delete(knife);
    }

    // Unlike the bulk-import/file-seed path (which auto-creates a stub Maker from
    // whatever name/slug a knife entry names), the dedicated admin Knife form
    // requires picking an existing Maker -- created via the Maker form first --
    // so a typo in makerSlug surfaces as a clear 404 instead of silently minting
    // a duplicate/incomplete Maker record.
    private Maker requireMaker(String makerSlug) {
        return makerRepository.findBySlug(makerSlug)
                .orElseThrow(() -> new IllegalArgumentException("No maker found with slug '" + makerSlug + "' -- create the maker first"));
    }

    private <T> T readSeedFile(String classpathLocation, TypeReference<T> type) throws Exception {
        try (InputStream in = new ClassPathResource(classpathLocation).getInputStream()) {
            return objectMapper.readValue(in, type);
        }
    }

    private Maker upsertMaker(MakerSeedDto dto) {
        Maker maker = makerRepository.findBySlug(dto.slug()).orElseGet(Maker::new);
        maker.setSlug(dto.slug());
        maker.setName(dto.name());
        if (dto.country() != null) maker.setCountry(dto.country());
        if (dto.knownFor() != null) maker.setKnownFor(dto.knownFor());
        if (dto.officialSiteUrl() != null) maker.setOfficialSiteUrl(dto.officialSiteUrl());
        if (dto.logoUrl() != null) maker.setLogoUrl(dto.logoUrl());
        if (dto.foundedYear() != null) maker.setFoundedYear(dto.foundedYear());
        if (dto.instagramUrl() != null) maker.setInstagramUrl(dto.instagramUrl());
        if (dto.youtubeUrl() != null) maker.setYoutubeUrl(dto.youtubeUrl());
        if (dto.facebookUrl() != null) maker.setFacebookUrl(dto.facebookUrl());
        if (dto.twitterUrl() != null) maker.setTwitterUrl(dto.twitterUrl());
        return makerRepository.save(maker);
    }

    private Knife seedKnife(KnifeSeedDto dto, Maker maker) {
        Knife knife = knifeRepository.findBySlug(dto.slug()).orElseGet(Knife::new);
        knife.setSlug(dto.slug());
        knife.setName(dto.name());
        knife.setMaker(maker);
        knife.setCoverPhotoUrl(dto.coverPhotoUrl());
        knife.setDescription(dto.description());
        // Wholesale replace, keyed by knife slug + version_slug uniqueness -- same
        // semantics whether this came from the file seed, bulk import, or a single
        // knife's admin edit form. Safe to re-run/re-save after fixing a mistake.
        // The flush is required: without it, Hibernate can order the new versions'
        // inserts before the old versions' orphan-removal deletes in the same batch,
        // tripping the (knife_id, version_slug) unique constraint when a version_slug
        // is reused across the clear.
        knife.getVersions().clear();
        knifeRepository.saveAndFlush(knife);

        for (VersionSeedDto v : dto.versions()) {
            knife.getVersions().add(buildVersion(v, knife));
        }

        return knifeRepository.save(knife);
    }

    private KnifeVersion buildVersion(VersionSeedDto v, Knife knife) {
        KnifeVersion version = new KnifeVersion();
        version.setKnife(knife);
        version.setVersionSlug(v.versionSlug());
        version.setVersionLabel(v.version());
        version.setDiscontinued(v.discontinued());
        version.setReleaseYear(v.releaseYear());
        version.setDescription(v.description());
        version.setOverallLength(parseDouble(v.overallLength()));
        version.setWeight(parseDouble(v.weight()));
        version.setPivotSystem(KnifeSpecNormalizer.pivotSystem(v.pivotSystem()));
        version.setLatchType(KnifeSpecNormalizer.latchType(v.latchType()));
        version.setPinSystem(KnifeSpecNormalizer.pinSystem(v.pinSystem()));
        version.setHasModularBalance(v.hasModularBalance());
        version.setBalanceValue(v.balanceValue());
        version.setHandleConstruction(KnifeSpecNormalizer.handleConstruction(v.handleConstruction()));
        version.setHandleMaterial(KnifeSpecNormalizer.handleMaterial(v.handleMaterial()));
        version.setHandleFinish(KnifeSpecNormalizer.handleFinish(v.handleFinish()));

        for (VariantSeedDto variantDto : v.variants()) {
            version.getVariants().add(buildVariant(variantDto, version));
        }

        int order = 0;
        for (WhereToFindSeedDto wtfDto : v.whereToFind()) {
            version.getWhereToFind().add(buildWhereToFind(wtfDto, version, order++));
        }

        return version;
    }

    private KnifeVariant buildVariant(VariantSeedDto dto, KnifeVersion version) {
        KnifeVariant variant = new KnifeVariant();
        variant.setKnifeVersion(version);
        variant.setVariantSlug(dto.variantSlug());
        KnifeType type = KnifeSpecNormalizer.variantType(dto.type());
        variant.setType(type);
        variant.setLabel(dto.label());
        variant.setMsrp(parseDouble(dto.msrp()));
        variant.setBladeStyle(type == KnifeType.TRAINER ? null : KnifeSpecNormalizer.bladeStyle(dto.bladeStyle()));
        variant.setBladeMaterial(KnifeSpecNormalizer.bladeMaterial(dto.bladeMaterial()));
        variant.setBladeFinish(KnifeSpecNormalizer.bladeFinish(dto.bladeFinish()));
        return variant;
    }

    private WhereToFind buildWhereToFind(WhereToFindSeedDto dto, KnifeVersion version, int sortOrder) {
        WhereToFind wtf = new WhereToFind();
        wtf.setKnifeVersion(version);
        wtf.setLabel(dto.label());
        wtf.setUrl(dto.url());
        wtf.setType(KnifeSpecNormalizer.sourceType(dto.type()));
        wtf.setNote(dto.note());
        wtf.setSortOrder(sortOrder);
        return wtf;
    }

    private Double parseDouble(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            log.warn("Could not parse numeric value '{}'", value);
            return null;
        }
    }
}
