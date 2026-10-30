package ru.library.management.repository;

import ru.library.management.model.BookCopy;
import ru.library.management.model.enums.CopyStatus;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface BookCopyRepository {
    BookCopy save(Connection connection, BookCopy copy) throws SQLException;

    Optional<BookCopy> findById(Connection connection, long id) throws SQLException;

    Optional<BookCopy> findByIdForUpdate(Connection connection, long id) throws SQLException;

    List<BookCopy> findByBookId(Connection connection, long bookId) throws SQLException;

    long countAvailableByBookId(Connection connection, long bookId) throws SQLException;

    void updateStatus(Connection connection, long copyId, CopyStatus status) throws SQLException;
}
