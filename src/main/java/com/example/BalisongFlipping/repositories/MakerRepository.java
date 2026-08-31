package com.example.BalisongFlipping.repositories;

import com.example.BalisongFlipping.modals.knifeCatalog.Maker;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MakerRepository extends JpaRepository<Maker, Long> {

    Optional<Maker> findBySlug(String slug);
}
