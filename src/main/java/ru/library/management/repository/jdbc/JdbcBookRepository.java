package ru.library.management.repository.jdbc;

import ru.library.management.model.Author;
import ru.library.management.model.Book;
import ru.library.management.repository.BookRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

public final class JdbcBookRepository implements BookRepository {
    @Override
    public Book save(Connection connection, Book book, List<String> authorNames) throws SQLException {
        String sql = """
                INSERT INTO books (isbn, title, publisher, publication_year)
                VALUES (?, ?, ?, ?)
                RETURNING id
                """;

        long bookId;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, book.isbn());
            statement.setString(2, book.title());
            statement.setString(3, book.publisher());
            if (book.year() == null) {
                statement.setNull(4, java.sql.Types.INTEGER);
            } else {
                statement.setInt(4, book.year());
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                bookId = resultSet.getLong("id");
            }
        }

        if (authorNames != null) {
            for (String name : new LinkedHashSet<>(authorNames)) {
                if (name == null || name.isBlank()) {
                    continue;
                }
                long authorId = findOrCreateAuthor(connection, name.trim());
                linkAuthor(connection, bookId, authorId);
            }
        }

        return findById(connection, bookId).orElseThrow(
                () -> new SQLException("Created book was not found: " + bookId)
        );
    }

    @Override
    public Optional<Book> findById(Connection connection, long id) throws SQLException {
        String sql = """
                SELECT id, isbn, title, publisher, publication_year
                FROM books
                WHERE id = ?
                """;
        BookRow row;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                row = mapRow(resultSet);
            }
        }
        return Optional.of(toBook(connection, row));
    }

    @Override
    public List<Book> findAll(Connection connection) throws SQLException {
        String sql = """
                SELECT id, isbn, title, publisher, publication_year
                FROM books
                ORDER BY title, id
                """;
        List<BookRow> rows = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                rows.add(mapRow(resultSet));
            }
        }
        List<Book> books = new ArrayList<>();
        for (BookRow row : rows) {
            books.add(toBook(connection, row));
        }
        return books;
    }

    private long findOrCreateAuthor(Connection connection, String fullName) throws SQLException {
        String sql = """
                INSERT INTO authors (full_name)
                VALUES (?)
                ON CONFLICT (full_name) DO UPDATE SET full_name = EXCLUDED.full_name
                RETURNING id
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, fullName);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong("id");
            }
        }
    }

    private void linkAuthor(Connection connection, long bookId, long authorId) throws SQLException {
        String sql = """
                INSERT INTO book_authors (book_id, author_id)
                VALUES (?, ?)
                ON CONFLICT DO NOTHING
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, bookId);
            statement.setLong(2, authorId);
            statement.executeUpdate();
        }
    }

    private BookRow mapRow(ResultSet resultSet) throws SQLException {
        int yearValue = resultSet.getInt("publication_year");
        Integer year = resultSet.wasNull() ? null : yearValue;
        return new BookRow(
                resultSet.getLong("id"), resultSet.getString("isbn"), resultSet.getString("title"),
                resultSet.getString("publisher"), year
        );
    }

    private Book toBook(Connection connection, BookRow row) throws SQLException {
        return new Book(
                row.id(), row.isbn(), row.title(), row.publisher(), row.year(),
                findAuthors(connection, row.id())
        );
    }

    private record BookRow(long id, String isbn, String title, String publisher, Integer year) {
    }

    private List<Author> findAuthors(Connection connection, long bookId) throws SQLException {
        String sql = """
                SELECT a.id, a.full_name
                FROM authors a
                JOIN book_authors ba ON ba.author_id = a.id
                WHERE ba.book_id = ?
                ORDER BY a.full_name
                """;
        List<Author> authors = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, bookId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    authors.add(new Author(resultSet.getLong("id"), resultSet.getString("full_name")));
                }
            }
        }
        return authors;
    }
}
