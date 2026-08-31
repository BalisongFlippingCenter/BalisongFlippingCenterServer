package com.example.BalisongFlipping.repositories;

import com.example.BalisongFlipping.modals.knifeCatalog.Knife;
import com.example.BalisongFlipping.modals.knifeCatalog.Maker;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KnifeRepository extends JpaRepository<Knife, Long> {

    Optional<Knife> findBySlug(String slug);

    List<Knife> findByNameContainingIgnoreCaseOrMakerNameContainingIgnoreCase(String name, String makerName);

    List<Knife> findByMaker(Maker maker);
}
