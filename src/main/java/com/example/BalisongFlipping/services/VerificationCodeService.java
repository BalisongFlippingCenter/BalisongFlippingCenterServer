package com.example.BalisongFlipping.services;

import com.example.BalisongFlipping.modals.accounts.Account;
import com.example.BalisongFlipping.modals.tokens.EmailVerificationToken;
import com.example.BalisongFlipping.repositories.EmailTokenRepository;
import com.example.BalisongFlipping.utils.RateLimiter;
import com.example.BalisongFlipping.utils.TooManyRequestsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;

@Service
public class VerificationCodeService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int MAX_CODES_PER_WINDOW = 3;
    private static final Duration CODE_WINDOW = Duration.ofMinutes(15);

    private final EmailTokenRepository emailTokenRepository;
    private final RateLimiter rateLimiter = new RateLimiter();

    public VerificationCodeService(EmailTokenRepository emailTokenRepository) {
        this.emailTokenRepository = emailTokenRepository;
    }

    @Transactional
    public String issue(Account account) {
        if (!rateLimiter.tryAcquire(account.getId().toString(), MAX_CODES_PER_WINDOW, CODE_WINDOW))
            throw new TooManyRequestsException("Too many codes requested. Please wait a few minutes and try again.");

        emailTokenRepository.deleteByOwner_Id(account.getId());
        return emailTokenRepository.save(new EmailVerificationToken(account)).getToken();
    }

    // a wrong guess throws a checked exception, which doesn't roll back, so the
    // failed-attempt count is kept even when the caller's transaction joins this one
    @Transactional(noRollbackFor = Exception.class)
    public void verify(Account account, String code) throws Exception {
        EmailVerificationToken token = emailTokenRepository.findFirstByOwner_IdOrderByIdDesc(account.getId())
                .orElseThrow(() -> new Exception("Invalid code."));

        if (token.getExpiration().isBefore(Instant.now()))
            throw new Exception("Code has expired. Please request a new one.");

        if (code == null || !MessageDigest.isEqual(
                token.getToken().getBytes(StandardCharsets.UTF_8), code.getBytes(StandardCharsets.UTF_8))) {
            if (token.getFailedAttempts() + 1 >= MAX_FAILED_ATTEMPTS) {
                emailTokenRepository.delete(token);
                throw new Exception("Too many incorrect attempts. Please request a new code.");
            }
            token.setFailedAttempts(token.getFailedAttempts() + 1);
            emailTokenRepository.save(token);
            throw new Exception("Invalid code.");
        }

        emailTokenRepository.delete(token);
    }
}
