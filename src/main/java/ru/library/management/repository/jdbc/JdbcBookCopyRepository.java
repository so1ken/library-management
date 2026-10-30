package ru.library.management.repository.jdbc;

import ru.library.management.model.BookCopy;
import ru.library.management.model.enums.CopyStatus;
import ru.library.management.repository.BookCopyRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class JdbcBookCopyRepository implements BookCopyRepository {
    @Override
    public BookCopy save(Connection connection, BookCopy copy) throws SQLException {
        String sql = """
                INSERT INTO book_copies (book_id, inventory_number, status, acquired_date)
                VALUES (?, ?, ?, ?)
                RETURNING id
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, copy.bookId());
            statement.setString(2, copy.inventoryNumber());
            statement.setString(3, copy.status().name());
            statement.setObject(4, copy.acquiredDate());
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return new BookCopy(
                        resultSet.getLong("id"), copy.bookId(), copy.inventoryNumber(),
                        copy.status(), copy.acquiredDate()
                );
            }
        }
    }

    @Override
    public Optional<BookCopy> findById(Connection connection, long id) throws SQLException {
        return find(connection, id, false);
    }

    @Override
    public Optional<BookCopy> findByIdForUpdate(Connection connection, long id) throws SQLException {
        return find(connection, id, true);
    }

    private Optional<BookCopy> find(Connection connection, long id, boolean forUpdate) throws SQLException {
        String sql = """
                SELECT id, book_id, inventory_number, status, acquired_date
                FROM book_copies
                WHERE id = ?
                """ + (forUpdate ? " FOR UPDATE" : "");
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        }
    }

    @Override
    public List<BookCopy> findByBookId(Connection connection, long bookId) throws SQLException {
        String sql = """
                SELECT id, book_id, inventory_number, status, acquired_date
                FROM book_copies
                WHERE book_id = ?
                ORDER BY inventory_number
                """;
        List<BookCopy> copies = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, bookId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    copies.add(map(resultSet));
                }
            }
        }
        return copies;
    }

    @Override
    public long countAvailableByBookId(Connection connection, long bookId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM book_copies WHERE book_id = ? AND status = 'AVAILABLE'";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, bookId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    @Override
    public void updateStatus(Connection connection, long copyId, CopyStatus status) throws SQLException {
        String sql = "UPDATE book_copies SET status = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status.name());
            statement.setLong(2, copyId);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Book copy not found: " + copyId);
            }
        }
    }

    private BookCopy map(ResultSet resultSet) throws SQLException {
        return new BookCopy(
                resultSet.getLong("id"),
                resultSet.getLong("book_id"),
                resultSet.getString("inventory_number"),
                CopyStatus.valueOf(resultSet.getString("status")),
                resultSet.getObject("acquired_date", java.time.LocalDate.class)
        );
    }
}
