package ru.library.management.repository;

import ru.library.management.model.Book;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface BookRepository {
    Book save(Connection connection, Book book, List<String> authorNames) throws SQLException;

    Optional<Book> findById(Connection connection, long id) throws SQLException;

    List<Book> findAll(Connection connection) throws SQLException;
}
