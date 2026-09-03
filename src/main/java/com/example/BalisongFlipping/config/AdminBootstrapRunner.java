package com.example.BalisongFlipping.config;

import com.example.BalisongFlipping.modals.accounts.Account;
import com.example.BalisongFlipping.repositories.AccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrapRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    @Value("${admin.bootstrap.email:}")
    private String bootstrapEmail;

    private final AccountRepository accountRepository;

    public AdminBootstrapRunner(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public void run(String... args) {
        tryBootstrap();
    }

    // Recheck periodically so a wiped/restored DB self-heals admin access without a restart —
    // the startup-only run() would otherwise require one after every reset.
    @Scheduled(fixedDelay = 180000)
    public void recheckBootstrap() {
        tryBootstrap();
    }

    private void tryBootstrap() {
        if (bootstrapEmail == null || bootstrapEmail.isBlank()) return;
        if (accountRepository.existsByRole("ADMIN")) return;

        accountRepository.findAccountByEmail(bootstrapEmail).ifPresent(account -> {
            account.setRole("ADMIN");
            accountRepository.save(account);
            log.info("Bootstrapped ADMIN role for {}", bootstrapEmail);
        });
    }
}
