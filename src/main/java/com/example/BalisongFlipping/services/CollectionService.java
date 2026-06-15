package com.example.BalisongFlipping.services;

import com.example.BalisongFlipping.dtos.CollectionDataDto;
import com.example.BalisongFlipping.dtos.UpdateKnifeDto;
import com.example.BalisongFlipping.modals.collectionKnives.CollectionKnife;
import com.example.BalisongFlipping.modals.collectionKnives.GalleryFile;
import com.example.BalisongFlipping.modals.collections.Collection;
import com.example.BalisongFlipping.repositories.CollectionKnifeRepository;
import com.example.BalisongFlipping.repositories.CollectionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CollectionService {
    private final CollectionRepository collectionRepository;
    private final CollectionKnifeRepository collectionKnifeRepository;

    @Autowired
    private PostService postService;

    public CollectionService(CollectionRepository collectionRepository, CollectionKnifeRepository collectionKnifeRepository) {
        this.collectionRepository = collectionRepository;
        this.collectionKnifeRepository = collectionKnifeRepository;
    }

    // -------------------------------------------------------------------------
    // DTO conversion
    // -------------------------------------------------------------------------

    private List<CollectionKnife> getCollectionKnives(Long collectionId) {
        return collectionKnifeRepository.findAllByCollectionId(collectionId)
                .orElse(new ArrayList<>());
    }

    private CollectionDataDto convertCollectionToDTO(Collection collection) {
        return new CollectionDataDto(
                collection.getId().toString(),
                collection.getUserId().toString(),
                collection.getBannerImg(),
                collection.getFeaturedKnife() != null ? collection.getFeaturedKnife().toString() : null,
                getCollectionKnives(collection.getId())
        );
    }

    // -------------------------------------------------------------------------
    // Collection lookups
    // -------------------------------------------------------------------------

    public CollectionDataDto getCollection(String collectionId) {
        Optional<Collection> foundCollection = collectionRepository.findById(Long.parseLong(collectionId));
        if (foundCollection.isEmpty()) return null;
        return convertCollectionToDTO(foundCollection.get());
    }

    public CollectionDataDto getCollectionByAccountId(String accountId) {
        Optional<Collection> foundCollection = collectionRepository.findByUserId(Long.parseLong(accountId));
        if (foundCollection.isEmpty()) return null;
        return convertCollectionToDTO(foundCollection.get());
    }

    public boolean checkForCollectionExistance(String collectionId) {
        return collectionRepository.findById(Long.parseLong(collectionId)).isPresent();
    }

    // -------------------------------------------------------------------------
    // Collection banner
    // -------------------------------------------------------------------------

    public CollectionDataDto updateBannerImg(String collectionId, String imageUrl) {
        Optional<Collection> foundCollection = collectionRepository.findById(Long.parseLong(collectionId));
        if (foundCollection.isEmpty()) return null;
        Collection collection = foundCollection.get();
        collection.setBannerImg(imageUrl);
        return convertCollectionToDTO(collectionRepository.save(collection));
    }

    // -------------------------------------------------------------------------
    // Featured knife
    // -------------------------------------------------------------------------

    public CollectionDataDto setFeaturedKnife(String collectionId, String knifeId) throws Exception {
        Collection collection = collectionRepository.findById(Long.parseLong(collectionId))
                .orElseThrow(() -> new Exception("Collection not found."));

        Long knifeIdLong = Long.parseLong(knifeId);

        // Verify the knife actually belongs to this collection
        boolean knifeExists = collectionKnifeRepository
                .findAllByCollectionId(collection.getId())
                .orElseThrow(() -> new Exception("Could not load knives."))
                .stream()
                .anyMatch(k -> k.getId().equals(knifeIdLong));

        if (!knifeExists) throw new Exception("Knife does not belong to this collection.");

        collection.setFeaturedKnife(knifeIdLong);
        return convertCollectionToDTO(collectionRepository.save(collection));
    }

    public CollectionDataDto clearFeaturedKnife(String collectionId) throws Exception {
        Collection collection = collectionRepository.findById(Long.parseLong(collectionId))
                .orElseThrow(() -> new Exception("Collection not found."));
        collection.setFeaturedKnife(null);
        return convertCollectionToDTO(collectionRepository.save(collection));
    }

    // -------------------------------------------------------------------------
    // Knife validation
    // -------------------------------------------------------------------------

    public boolean validateNewKnifeInfo(String displayName, String knifeMaker, String baseKnifeModel,
                                        String knifeType, String aqquiredDate, MultipartFile coverPhoto) {
        return !displayName.isEmpty() && !knifeMaker.isEmpty() && !baseKnifeModel.isEmpty()
                && !knifeType.isEmpty() && !aqquiredDate.isEmpty() && !coverPhoto.isEmpty();
    }

    // Used on add — checks all knives in the collection
    public boolean checkForDuplicateDisplayName(String displayName, String collectionId) throws Exception {
        return checkForDuplicateDisplayName(displayName, collectionId, null);
    }

    // Used on update — excludes the knife being edited so renaming to its current name passes
    public boolean checkForDuplicateDisplayName(String displayName, String collectionId, Long excludeKnifeId) throws Exception {
        List<CollectionKnife> knives = collectionKnifeRepository
                .findAllByCollectionId(Long.parseLong(collectionId))
                .orElse(new ArrayList<>());

        for (CollectionKnife knife : knives) {
            if (excludeKnifeId != null && knife.getId().equals(excludeKnifeId)) continue;
            if (knife.getDisplayName().equals(displayName)) return true;
        }

        return false;
    }

    // -------------------------------------------------------------------------
    // Knife lookup helpers
    // -------------------------------------------------------------------------

    public CollectionKnife getKnifeById(Long knifeId) {
        return collectionKnifeRepository.findById(knifeId).orElse(null);
    }

    public String getKnifeDisplayName(String collectionId, String knifeId) throws Exception {
        CollectionKnife knife = collectionKnifeRepository.findById(Long.parseLong(knifeId))
                .orElseThrow(() -> new Exception("Knife not found."));
        if (!knife.getCollectionId().equals(Long.parseLong(collectionId)))
            throw new Exception("Knife does not belong to this collection.");
        return knife.getDisplayName().replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    // -------------------------------------------------------------------------
    // Update knife cover photo
    // -------------------------------------------------------------------------

    public CollectionKnife updateKnifeCoverPhoto(String collectionId, String knifeId, String coverPhotoUrl) throws Exception {
        Long knifeIdLong = Long.parseLong(knifeId);

        CollectionKnife knife = collectionKnifeRepository.findById(knifeIdLong)
                .orElseThrow(() -> new Exception("Knife not found."));

        if (!knife.getCollectionId().equals(Long.parseLong(collectionId)))
            throw new Exception("Knife does not belong to this collection.");

        knife.setCoverPhoto(coverPhotoUrl);
        return collectionKnifeRepository.save(knife);
    }

    // -------------------------------------------------------------------------
    // Delete knife
    // -------------------------------------------------------------------------

    public void deleteKnife(String collectionId, String knifeId) throws Exception {
        Long knifeIdLong = Long.parseLong(knifeId);
        Long collectionIdLong = Long.parseLong(collectionId);

        CollectionKnife knife = collectionKnifeRepository.findById(knifeIdLong)
                .orElseThrow(() -> new Exception("Knife not found."));

        if (!knife.getCollectionId().equals(collectionIdLong))
            throw new Exception("Knife does not belong to this collection.");

        // If this knife was the featured knife, clear it
        collectionRepository.findById(collectionIdLong).ifPresent(collection -> {
            if (knifeIdLong.equals(collection.getFeaturedKnife())) {
                collection.setFeaturedKnife(null);
                collectionRepository.save(collection);
            }
        });

        collectionKnifeRepository.deleteById(knifeIdLong);
    }

    // -------------------------------------------------------------------------
    // Update knife
    // -------------------------------------------------------------------------

    public CollectionKnife updateKnife(String collectionId, String knifeId, UpdateKnifeDto dto) throws Exception {
        Long knifeIdLong = Long.parseLong(knifeId);
        Long collectionIdLong = Long.parseLong(collectionId);

        // Load and verify ownership
        CollectionKnife knife = collectionKnifeRepository.findById(knifeIdLong)
                .orElseThrow(() -> new Exception("Knife not found."));

        if (!knife.getCollectionId().equals(collectionIdLong))
            throw new Exception("Knife does not belong to this collection.");

        // Validate display name uniqueness (excluding this knife)
        if (checkForDuplicateDisplayName(dto.displayName(), collectionId, knifeIdLong))
            throw new Exception("A knife with that display name already exists in your collection.");

        // Apply all editable fields using the existing string-parsing setters
        knife.setDisplayName(dto.displayName());
        knife.setKnifeMaker(dto.knifeMaker());
        knife.setBaseKnifeModel(dto.baseKnifeModel());
        knife.setKnifeType(dto.knifeType());
        knife.setAqquiredDate(dto.aqquiredDate());
        knife.setIsFavoriteKnife(dto.isFavoriteKnife());
        knife.setIsFavoriteFlipper(dto.isFavoriteFlipper());
        knife.setMsrp(dto.knifeMSRP());
        knife.setOverallLength(dto.overallLength());
        knife.setWeight(dto.weight());
        knife.setPivotSystem(dto.pivotSystem());
        knife.setLatchType(dto.latchType());
        knife.setPinSystem(dto.pinSystem());
        knife.setHasModularBalance(String.valueOf(dto.hasModularBalance()));
        knife.setBalanceValue(dto.balanceValue());
        knife.setBladeStyle(dto.bladeStyle());
        knife.setBladeFinish(dto.bladeFinish());
        knife.setBladeMaterial(dto.bladeMaterial());
        knife.setHandleConstruction(dto.handleConstruction());
        knife.setHandleMaterial(dto.handleMaterial());
        knife.setHandleFinish(dto.handleFinish());

        // Apply scores and recalculate average
        knife.setQualityScore(dto.qualityScore());
        knife.setFlippingScore(dto.flippingScore());
        knife.setFeelScore(dto.feelScore());
        knife.setSoundScore(dto.soundScore());
        knife.setDurabilityScore(dto.durabilityScore());
        knife.setAverageScore(
                (dto.qualityScore() + dto.flippingScore() + dto.feelScore() + dto.soundScore() + dto.durabilityScore()) / 5.0
        );

        return collectionKnifeRepository.save(knife);
    }

    // -------------------------------------------------------------------------
    // Add knife
    // -------------------------------------------------------------------------

    public CollectionKnife addNewKnife(
            String collectionId,
            String displayName,
            String knifeMaker,
            String baseKnifeModel,
            String knifeType,
            String aqquiredDate,
            String isFavoriteKnife,
            String isFavoriteFlipper,
            String coverPhotoUrl,
            String knifeMSRP,
            String overallLength,
            String weight,
            String pivotSystem,
            String latchType,
            String pinSystem,
            String hasModularBalance,
            String balanceValue,
            String bladeStyle,
            String bladeFinish,
            String bladeMaterial,
            String handleConstruction,
            String handleMaterial,
            String handleFinish,
            double qualityScore,
            double flippingScore,
            double feelScore,
            double soundScore,
            double durabilityScore,
            List<String> galleryUrls
    ) throws Exception {
        // Build gallery file list from S3 URLs
        List<GalleryFile> gallery = new ArrayList<>();
        if (galleryUrls != null) {
            for (String url : galleryUrls) {
                gallery.add(new GalleryFile(url, null));
            }
        }

        CollectionKnife newKnife = new CollectionKnife(
                collectionId, displayName, knifeMaker, baseKnifeModel, knifeType,
                aqquiredDate, isFavoriteKnife, isFavoriteFlipper, coverPhotoUrl,
                knifeMSRP, overallLength, weight, pivotSystem, latchType, pinSystem,
                hasModularBalance, balanceValue, bladeStyle, bladeFinish, bladeMaterial,
                handleConstruction, handleMaterial, handleFinish,
                qualityScore, flippingScore, feelScore, soundScore, durabilityScore,
                gallery
        );

        return collectionKnifeRepository.save(newKnife);
    }
}
