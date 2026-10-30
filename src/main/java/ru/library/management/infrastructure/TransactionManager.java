package ru.library.management.infrastructure;

import com.zaxxer.hikari.HikariDataSource;
import ru.library.management.exception.DataAccessException;

import java.sql.Connection;
import java.sql.SQLException;

public final class TransactionManager {
    private final HikariDataSource dataSource;

    public TransactionManager(HikariDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public <T> T inTransaction(TransactionWork<T> work) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                T result = work.execute(connection);
                connection.commit();
                return result;
            } catch (Throwable exception) {
                rollback(connection, exception);
                if (exception instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                }
                if (exception instanceof Error error) {
                    throw error;
                }
                throw new DataAccessException("Database operation failed: " + exception.getMessage(), exception);
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Could not execute database transaction", exception);
        }
    }

    private void rollback(Connection connection, Throwable originalException) {
        try {
            connection.rollback();
        } catch (SQLException rollbackException) {
            originalException.addSuppressed(rollbackException);
        }
    }

    @FunctionalInterface
    public interface TransactionWork<T> {
        T execute(Connection connection) throws Exception;
    }
}
