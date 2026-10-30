package ru.library.management.infrastructure;

import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;

public final class FlywayMigration {
    private FlywayMigration() {
    }

    public static void migrate(HikariDataSource dataSource) {
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate();
    }
}
