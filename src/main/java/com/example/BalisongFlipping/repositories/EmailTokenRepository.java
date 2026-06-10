package com.example.BalisongFlipping.repositories;

import com.example.BalisongFlipping.modals.tokens.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    void deleteByOwner_Id(Long id);

    Optional<EmailVerificationToken> findByToken(String token);
}
