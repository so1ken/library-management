package ru.library.management.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DatabaseConnection implements AutoCloseable {
    private final HikariDataSource dataSource;

    public DatabaseConnection() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(setting("DB_URL", "jdbc:postgresql://localhost:5432/library_db"));
        config.setUsername(setting("DB_USER", "library_user"));
        config.setPassword(setting("DB_PASSWORD", "library_password"));
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(10_000);
        config.setPoolName("library-management-pool");
        this.dataSource = new HikariDataSource(config);
    }

    public HikariDataSource getDataSource() {
        return dataSource;
    }

    private static String setting(String name, String defaultValue) {
        String environmentValue = System.getenv(name);
        if (environmentValue != null && !environmentValue.isBlank()) {
            return environmentValue;
        }

        String fileValue = readDotEnv().get(name);
        return fileValue == null || fileValue.isBlank() ? defaultValue : fileValue;
    }

    private static Map<String, String> readDotEnv() {
        Path envFile = Path.of(".env");
        if (!Files.isRegularFile(envFile)) {
            return Map.of();
        }

        Map<String, String> values = new HashMap<>();
        try {
            List<String> lines = Files.readAllLines(envFile);
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                int separator = trimmed.indexOf('=');
                if (separator <= 0) {
                    continue;
                }
                String key = trimmed.substring(0, separator).trim();
                String value = trimmed.substring(separator + 1).trim();
                if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                }
                values.put(key, value);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read .env file", exception);
        }
        return values;
    }

    @Override
    public void close() {
        dataSource.close();
    }
}
