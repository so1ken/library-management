package ru.library.management.repository;

import ru.library.management.model.Loan;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LoanRepository {
    Loan save(Connection connection, Loan loan) throws SQLException;

    Optional<Loan> findActiveByCopyId(Connection connection, long copyId) throws SQLException;

    void markReturned(Connection connection, long loanId, LocalDateTime returnedAt) throws SQLException;

    void markOverdueLoans(Connection connection, LocalDateTime now) throws SQLException;

    List<Loan> findActiveLoans(Connection connection) throws SQLException;
}
