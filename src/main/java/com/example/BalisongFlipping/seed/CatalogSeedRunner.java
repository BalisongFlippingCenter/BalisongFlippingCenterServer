package com.example.BalisongFlipping.seed;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * One-time bootstrap: loads the original static catalog (src/main/resources/seed-data) into
 * the knife_catalog tables. Idempotent — re-running replaces each knife's versions wholesale,
 * keyed by slug, so it's safe to run again after editing the seed JSON during development.
 *
 * Invoke with the "seed-catalog" profile active, e.g.:
 *   docker-compose run --rm -e SPRING_PROFILES_ACTIVE=seed-catalog server
 */
@Component
@Profile("seed-catalog")
public class CatalogSeedRunner implements CommandLineRunner {

    private final CatalogSeedService seedService;
    private final ConfigurableApplicationContext context;

    public CatalogSeedRunner(CatalogSeedService seedService, ConfigurableApplicationContext context) {
        this.seedService = seedService;
        this.context = context;
    }

    @Override
    public void run(String... args) throws Exception {
        seedService.seed();
        System.exit(SpringApplication.exit(context, () -> 0));
    }
}
