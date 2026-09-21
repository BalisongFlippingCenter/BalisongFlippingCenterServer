package com.example.BalisongFlipping.seed;

import com.example.BalisongFlipping.dtos.catalogSeedDtos.CatalogImageUploadUrlRequestDto;
import com.example.BalisongFlipping.dtos.catalogSeedDtos.KnifeSeedDto;
import com.example.BalisongFlipping.dtos.catalogSeedDtos.MakerSeedDto;
import com.example.BalisongFlipping.dtos.catalogSeedDtos.VariantSeedDto;
import com.example.BalisongFlipping.dtos.catalogSeedDtos.VersionSeedDto;
import com.example.BalisongFlipping.dtos.uploadsDtos.PresignedUploadTargetDto;
import com.example.BalisongFlipping.modals.knifeCatalog.Knife;
import com.example.BalisongFlipping.modals.knifeCatalog.Maker;
import com.example.BalisongFlipping.repositories.KnifeRepository;
import com.example.BalisongFlipping.repositories.MakerRepository;
import com.example.BalisongFlipping.services.S3Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogSeedServiceTest {

    @Mock private MakerRepository makerRepository;
    @Mock private KnifeRepository knifeRepository;
    @Mock private S3Service s3Service;

    private CatalogSeedService catalogSeedService;

    @BeforeEach
    void setUp() {
        catalogSeedService = new CatalogSeedService(makerRepository, knifeRepository, new ObjectMapper());
        ReflectionTestUtils.setField(catalogSeedService, "s3Service", s3Service);
        ReflectionTestUtils.setField(catalogSeedService, "bucketName", "test-bucket");
        ReflectionTestUtils.setField(catalogSeedService, "s3Region", "us-east-1");
    }

    private MakerSeedDto makerSeed(String slug) {
        return new MakerSeedDto(slug, "Squid Industries", "USA", "Trainers", null, null, 2014, null, null, null, null);
    }

    private VariantSeedDto trainerVariant(String slug) {
        return new VariantSeedDto(slug, "trainer", "Standard", "150", null, null, null);
    }

    private VariantSeedDto liveBladeVariant(String slug, String bladeStyle, String bladeMaterial) {
        return new VariantSeedDto(slug, "live", "Live Blade", "200", bladeStyle, bladeMaterial, null);
    }

    private VersionSeedDto validVersion(List<VariantSeedDto> variants) {
        return new VersionSeedDto("v1", "Version 1", false, 2020, "First release",
                "9.5", "3.2", "bushings", "latchless", "zen pins",
                false, null, "channel", "g10", "stonewash",
                variants, List.of());
    }

    private KnifeSeedDto knifeSeed(String slug, String makerSlug, List<VersionSeedDto> versions) {
        return new KnifeSeedDto(slug, "Mako", "Squid Industries", makerSlug, "Reverse Tanto",
                "$150-$200", null, "A great trainer.", versions);
    }

    // -------------------------------------------------------------------------
    // importCatalog
    // -------------------------------------------------------------------------

    @Test
    void importCatalogUpsertsMakersAndKnives() {
        when(makerRepository.findBySlug(any())).thenReturn(Optional.empty());
        when(makerRepository.save(any(Maker.class))).thenAnswer(i -> i.getArgument(0));
        when(knifeRepository.findBySlug(any())).thenReturn(Optional.empty());
        when(knifeRepository.saveAndFlush(any(Knife.class))).thenAnswer(i -> i.getArgument(0));
        when(knifeRepository.save(any(Knife.class))).thenAnswer(i -> i.getArgument(0));

        KnifeSeedDto knife = knifeSeed("mako", "squid-industries", List.of(validVersion(List.of(trainerVariant("standard")))));

        catalogSeedService.importCatalog(List.of(makerSeed("squid-industries")), List.of(knife));

        verify(makerRepository, times(2)).save(any(Maker.class)); // once from the maker list, once from the knife's stub maker upsert
        verify(knifeRepository).save(any(Knife.class));
    }

    // -------------------------------------------------------------------------
    // createMaker / updateMaker / deleteMaker
    // -------------------------------------------------------------------------

    @Test
    void createMakerSucceeds() {
        when(makerRepository.findBySlug("squid-industries")).thenReturn(Optional.empty());
        when(makerRepository.save(any(Maker.class))).thenAnswer(i -> i.getArgument(0));

        Maker maker = catalogSeedService.createMaker(makerSeed("squid-industries"));

        assertEquals("squid-industries", maker.getSlug());
        assertEquals("Squid Industries", maker.getName());
    }

    @Test
    void createMakerRejectsDuplicateSlug() {
        when(makerRepository.findBySlug("squid-industries")).thenReturn(Optional.of(new Maker()));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> catalogSeedService.createMaker(makerSeed("squid-industries")));
        assertTrue(ex.getMessage().contains("already exists"));
    }

    @Test
    void updateMakerKeepsSlugFromPathIgnoringBody() {
        when(makerRepository.findBySlug("squid-industries")).thenReturn(Optional.of(new Maker()));
        when(makerRepository.save(any(Maker.class))).thenAnswer(i -> i.getArgument(0));

        Maker updated = catalogSeedService.updateMaker("squid-industries",
                new MakerSeedDto("different-slug-in-body", "New Name", null, null, null, null, null, null, null, null, null));

        assertEquals("squid-industries", updated.getSlug());
        assertEquals("New Name", updated.getName());
    }

    @Test
    void updateMakerRejectsUnknownSlug() {
        when(makerRepository.findBySlug("bogus")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> catalogSeedService.updateMaker("bogus", makerSeed("bogus")));
    }

    @Test
    void deleteMakerSucceeds() {
        Maker maker = new Maker();
        when(makerRepository.findBySlug("squid-industries")).thenReturn(Optional.of(maker));
        when(knifeRepository.findByMaker(maker)).thenReturn(List.of());

        catalogSeedService.deleteMaker("squid-industries");

        verify(makerRepository).delete(maker);
    }

    @Test
    void deleteMakerRejectsWhenKnivesReferenceIt() {
        Maker maker = new Maker();
        when(makerRepository.findBySlug("squid-industries")).thenReturn(Optional.of(maker));
        when(knifeRepository.findByMaker(maker)).thenReturn(List.of(new Knife()));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> catalogSeedService.deleteMaker("squid-industries"));
        assertTrue(ex.getMessage().contains("still has knives"));
    }

    @Test
    void deleteMakerRejectsUnknownSlug() {
        when(makerRepository.findBySlug("bogus")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> catalogSeedService.deleteMaker("bogus"));
    }

    // -------------------------------------------------------------------------
    // createKnife / updateKnife / deleteKnife
    // -------------------------------------------------------------------------

    @Test
    void createKnifeSucceedsAndNormalizesSpecs() {
        Maker maker = new Maker();
        maker.setSlug("squid-industries");
        when(knifeRepository.findBySlug("mako")).thenReturn(Optional.empty());
        when(makerRepository.findBySlug("squid-industries")).thenReturn(Optional.of(maker));
        when(knifeRepository.saveAndFlush(any(Knife.class))).thenAnswer(i -> i.getArgument(0));
        when(knifeRepository.save(any(Knife.class))).thenAnswer(i -> i.getArgument(0));

        Knife knife = catalogSeedService.createKnife(
                knifeSeed("mako", "squid-industries", List.of(validVersion(List.of(trainerVariant("standard"))))));

        assertEquals("mako", knife.getSlug());
        assertEquals(1, knife.getVersions().size());
        assertEquals(com.example.BalisongFlipping.enums.knives.PivotSystem.BUSHINGS, knife.getVersions().get(0).getPivotSystem());
    }

    @Test
    void createKnifeRejectsDuplicateSlug() {
        when(knifeRepository.findBySlug("mako")).thenReturn(Optional.of(new Knife()));

        assertThrows(IllegalStateException.class, () -> catalogSeedService.createKnife(
                knifeSeed("mako", "squid-industries", List.of(validVersion(List.of(trainerVariant("standard")))))));
    }

    @Test
    void createKnifeRejectsUnknownMakerSlug() {
        when(knifeRepository.findBySlug("mako")).thenReturn(Optional.empty());
        when(makerRepository.findBySlug("bogus-maker")).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> catalogSeedService.createKnife(
                knifeSeed("mako", "bogus-maker", List.of(validVersion(List.of(trainerVariant("standard")))))));
        assertTrue(ex.getMessage().contains("create the maker first"));
    }

    @Test
    void createKnifeRejectsMissingRequiredVersionField() {
        when(knifeRepository.findBySlug("mako")).thenReturn(Optional.empty());
        when(makerRepository.findBySlug("squid-industries")).thenReturn(Optional.of(new Maker()));

        VersionSeedDto incompleteVersion = new VersionSeedDto("v1", "Version 1", false, 2020, null,
                null, "3.2", "bushings", "latchless", "zen pins",
                false, null, "channel", "g10", "stonewash",
                List.of(trainerVariant("standard")), List.of());

        CatalogValidationException ex = assertThrows(CatalogValidationException.class, () -> catalogSeedService.createKnife(
                knifeSeed("mako", "squid-industries", List.of(incompleteVersion))));
        assertTrue(ex.getMessage().contains("overallLength"));
    }

    @Test
    void createKnifeRejectsLiveBladeVariantMissingBladeSpecs() {
        when(knifeRepository.findBySlug("mako")).thenReturn(Optional.empty());
        when(makerRepository.findBySlug("squid-industries")).thenReturn(Optional.of(new Maker()));

        VariantSeedDto incompleteLiveVariant = new VariantSeedDto("live", "live", "Live Blade", "200", null, null, null);

        CatalogValidationException ex = assertThrows(CatalogValidationException.class, () -> catalogSeedService.createKnife(
                knifeSeed("mako", "squid-industries", List.of(validVersion(List.of(incompleteLiveVariant))))));
        assertTrue(ex.getMessage().contains("bladeStyle"));
        assertTrue(ex.getMessage().contains("bladeMaterial"));
    }

    @Test
    void createKnifeAcceptsLiveBladeVariantWithFullSpecs() {
        when(knifeRepository.findBySlug("mako")).thenReturn(Optional.empty());
        when(makerRepository.findBySlug("squid-industries")).thenReturn(Optional.of(new Maker()));
        when(knifeRepository.saveAndFlush(any(Knife.class))).thenAnswer(i -> i.getArgument(0));
        when(knifeRepository.save(any(Knife.class))).thenAnswer(i -> i.getArgument(0));

        Knife knife = catalogSeedService.createKnife(knifeSeed("mako", "squid-industries",
                List.of(validVersion(List.of(liveBladeVariant("live", "drop point", "m390"))))));

        assertEquals(com.example.BalisongFlipping.enums.knives.BladeStyle.DROP_POINT,
                knife.getVersions().get(0).getVariants().get(0).getBladeStyle());
        assertEquals(com.example.BalisongFlipping.enums.knives.BladeMaterial.M390,
                knife.getVersions().get(0).getVariants().get(0).getBladeMaterial());
    }

    @Test
    void updateKnifeSucceedsAndIgnoresBodySlug() {
        Maker maker = new Maker();
        when(knifeRepository.findBySlug("mako")).thenReturn(Optional.of(new Knife()));
        when(makerRepository.findBySlug("squid-industries")).thenReturn(Optional.of(maker));
        when(knifeRepository.saveAndFlush(any(Knife.class))).thenAnswer(i -> i.getArgument(0));
        when(knifeRepository.save(any(Knife.class))).thenAnswer(i -> i.getArgument(0));

        Knife knife = catalogSeedService.updateKnife("mako",
                knifeSeed("different-slug-in-body", "squid-industries", List.of(validVersion(List.of(trainerVariant("standard"))))));

        assertEquals("mako", knife.getSlug());
    }

    @Test
    void updateKnifeRejectsUnknownSlug() {
        when(knifeRepository.findBySlug("bogus")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> catalogSeedService.updateKnife("bogus",
                knifeSeed("bogus", "squid-industries", List.of(validVersion(List.of(trainerVariant("standard")))))));
    }

    @Test
    void deleteKnifeSucceeds() {
        Knife knife = new Knife();
        when(knifeRepository.findBySlug("mako")).thenReturn(Optional.of(knife));

        catalogSeedService.deleteKnife("mako");

        verify(knifeRepository).delete(knife);
    }

    @Test
    void deleteKnifeRejectsUnknownSlug() {
        when(knifeRepository.findBySlug("bogus")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> catalogSeedService.deleteKnife("bogus"));
    }

    // -------------------------------------------------------------------------
    // generateImageUploadUrl
    // -------------------------------------------------------------------------

    @Test
    void generateImageUploadUrlForCoverPhoto() throws Exception {
        when(s3Service.generatePresignedUploadUrl(any(), any(), any(), any())).thenReturn("https://upload");

        PresignedUploadTargetDto result = catalogSeedService.generateImageUploadUrl(
                new CatalogImageUploadUrlRequestDto("mako", null, null, "cover.png", "image/png"));

        assertTrue(result.key().contains("catalog/knives/mako/cover/"));
        assertEquals("https://upload", result.uploadUrl());
    }

    @Test
    void generateImageUploadUrlForVariantImage() throws Exception {
        when(s3Service.generatePresignedUploadUrl(any(), any(), any(), any())).thenReturn("https://upload");

        PresignedUploadTargetDto result = catalogSeedService.generateImageUploadUrl(
                new CatalogImageUploadUrlRequestDto("mako", "v1", "live", "photo.png", "image/png"));

        assertTrue(result.key().contains("catalog/knives/mako/v1/live/"));
    }

    @Test
    void generateImageUploadUrlRejectsMissingKnifeSlug() {
        Exception ex = assertThrows(Exception.class, () -> catalogSeedService.generateImageUploadUrl(
                new CatalogImageUploadUrlRequestDto(null, null, null, "cover.png", "image/png")));
        assertEquals("knifeSlug is required.", ex.getMessage());
    }

    @Test
    void generateImageUploadUrlRejectsMissingFilename() {
        Exception ex = assertThrows(Exception.class, () -> catalogSeedService.generateImageUploadUrl(
                new CatalogImageUploadUrlRequestDto("mako", null, null, null, "image/png")));
        assertEquals("filename is required.", ex.getMessage());
    }
}
