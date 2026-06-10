package com.example.BalisongFlipping.repositories;

import com.example.BalisongFlipping.modals.collections.Collection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CollectionRepository extends JpaRepository<Collection, Long> {

    Optional<Collection> findByUserId(Long userId);
}
