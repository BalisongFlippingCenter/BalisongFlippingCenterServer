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
        for (MakerSeedDto dto : makerDtos) {
            upsertMaker(dto.slug(), dto.name(), dto.country(), dto.knownFor());
        }

        List<KnifeSeedDto> knifeDtos = readSeedFile("seed-data/knives.json", new TypeReference<List<KnifeSeedDto>>() {});
        for (KnifeSeedDto dto : knifeDtos) {
            Maker maker = upsertMaker(dto.makerSlug(), dto.maker(), null, null);
            seedKnife(dto, maker);
        }

        log.info("Catalog seed complete: {} makers, {} knives", makerRepository.count(), knifeRepository.count());
    }

    private <T> T readSeedFile(String classpathLocation, TypeReference<T> type) throws Exception {
        try (InputStream in = new ClassPathResource(classpathLocation).getInputStream()) {
            return objectMapper.readValue(in, type);
        }
    }

    private Maker upsertMaker(String slug, String name, String country, String knownFor) {
        Maker maker = makerRepository.findBySlug(slug).orElseGet(Maker::new);
        maker.setSlug(slug);
        maker.setName(name);
        if (country != null) maker.setCountry(country);
        if (knownFor != null) maker.setKnownFor(knownFor);
        return makerRepository.save(maker);
    }

    private void seedKnife(KnifeSeedDto dto, Maker maker) {
        Knife knife = knifeRepository.findBySlug(dto.slug()).orElseGet(Knife::new);
        knife.setSlug(dto.slug());
        knife.setName(dto.name());
        knife.setMaker(maker);
        knife.getVersions().clear();

        for (VersionSeedDto v : dto.versions()) {
            knife.getVersions().add(buildVersion(v, knife));
        }

        knifeRepository.save(knife);
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
