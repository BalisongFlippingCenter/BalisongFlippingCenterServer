package com.example.BalisongFlipping.repositories;

import com.example.BalisongFlipping.modals.tokens.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    void deleteByOwner_Id(Long id);

    Optional<RefreshToken> findByToken(String token);
}
