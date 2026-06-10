package com.example.BalisongFlipping.repositories;

import com.example.BalisongFlipping.modals.collectionKnives.CollectionKnife;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CollectionKnifeRepository extends JpaRepository<CollectionKnife, Long> {

    Optional<List<CollectionKnife>> findAllByCollectionId(Long collectionId);

    void deleteAllByCollectionId(Long collectionId);
}
