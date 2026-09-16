package com.example.BalisongFlipping.config;

import com.example.BalisongFlipping.modals.accounts.Account;
import com.example.BalisongFlipping.repositories.AccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Optional;

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

        Optional<Account> existingAdmin = accountRepository.findFirstByRole("ADMIN");
        if (existingAdmin.isPresent()) {
            // This runner never demotes an existing admin -- if ADMIN_BOOTSTRAP_EMAIL has drifted
            // from whoever actually holds the role (e.g. a placeholder value never corrected, or
            // the role changed by hand), that drift is otherwise completely silent. Surfacing it
            // here is what would have caught this incident: an admin login test, not a config diff.
            if (!existingAdmin.get().getEmail().equalsIgnoreCase(bootstrapEmail)) {
                log.warn("ADMIN_BOOTSTRAP_EMAIL is set to '{}' but the current ADMIN is '{}' -- " +
                                "if that's not intentional, ADMIN_BOOTSTRAP_EMAIL is likely misconfigured. " +
                                "This runner never auto-demotes an existing admin, so fixing the value alone won't correct it.",
                        bootstrapEmail, existingAdmin.get().getEmail());
            }
            return;
        }

        accountRepository.findAccountByEmail(bootstrapEmail).ifPresent(account -> {
            account.setRole("ADMIN");
            accountRepository.save(account);
            log.info("Bootstrapped ADMIN role for {}", bootstrapEmail);
        });
    }
}
