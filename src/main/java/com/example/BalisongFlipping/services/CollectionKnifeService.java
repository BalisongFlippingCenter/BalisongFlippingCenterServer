package com.example.BalisongFlipping.services;

import com.example.BalisongFlipping.modals.collectionKnives.CollectionKnife;
import com.example.BalisongFlipping.repositories.CollectionKnifeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CollectionKnifeService {

    @Autowired
    private CollectionKnifeRepository collectionKnifeRepository;

    public String getCollectionKnifeCoverPhoto(String collectionKnifeId) throws Exception {
        Optional<CollectionKnife> foundKnife = collectionKnifeRepository.findById(Long.parseLong(collectionKnifeId));

        if (foundKnife.isPresent()) {
            return foundKnife.get().getCoverPhoto();
        }

        throw new Exception("Couldn't locate collection knife data.");
    }

    public String getCollectionKnifeDisplayName(String collectionKnifeId) throws Exception {
        Optional<CollectionKnife> foundKnife = collectionKnifeRepository.findById(Long.parseLong(collectionKnifeId));

        if (foundKnife.isPresent()) {
            return foundKnife.get().getDisplayName();
        }

        throw new Exception("Couldn't locate collection knife data.");
    }
}
