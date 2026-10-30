package ru.library.management.repository.jdbc;

import ru.library.management.model.Loan;
import ru.library.management.model.enums.LoanStatus;
import ru.library.management.repository.LoanRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class JdbcLoanRepository implements LoanRepository {
    @Override
    public Loan save(Connection connection, Loan loan) throws SQLException {
        String sql = """
                INSERT INTO loans (copy_id, member_id, loaned_at, due_at, returned_at, status)
                VALUES (?, ?, ?, ?, ?, ?)
                RETURNING id
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, loan.copyId());
            statement.setLong(2, loan.memberId());
            statement.setTimestamp(3, Timestamp.valueOf(loan.loanedAt()));
            statement.setTimestamp(4, Timestamp.valueOf(loan.dueAt()));
            if (loan.returnedAt() == null) {
                statement.setNull(5, java.sql.Types.TIMESTAMP);
            } else {
                statement.setTimestamp(5, Timestamp.valueOf(loan.returnedAt()));
            }
            statement.setString(6, loan.status().name());
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return new Loan(
                        resultSet.getLong("id"), loan.copyId(), loan.memberId(), loan.loanedAt(),
                        loan.dueAt(), loan.returnedAt(), loan.status()
                );
            }
        }
    }

    @Override
    public Optional<Loan> findActiveByCopyId(Connection connection, long copyId) throws SQLException {
        String sql = """
                SELECT id, copy_id, member_id, loaned_at, due_at, returned_at, status
                FROM loans
                WHERE copy_id = ? AND status IN ('ACTIVE', 'OVERDUE')
                ORDER BY loaned_at DESC
                LIMIT 1
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, copyId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        }
    }

    @Override
    public void markReturned(Connection connection, long loanId, LocalDateTime returnedAt) throws SQLException {
        String sql = """
                UPDATE loans
                SET status = 'RETURNED', returned_at = ?
                WHERE id = ? AND status IN ('ACTIVE', 'OVERDUE')
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, Timestamp.valueOf(returnedAt));
            statement.setLong(2, loanId);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Active loan not found: " + loanId);
            }
        }
    }

    @Override
    public void markOverdueLoans(Connection connection, LocalDateTime now) throws SQLException {
        String sql = "UPDATE loans SET status = 'OVERDUE' WHERE status = 'ACTIVE' AND due_at < ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, Timestamp.valueOf(now));
            statement.executeUpdate();
        }
    }

    @Override
    public List<Loan> findActiveLoans(Connection connection) throws SQLException {
        String sql = """
                SELECT id, copy_id, member_id, loaned_at, due_at, returned_at, status
                FROM loans
                WHERE status IN ('ACTIVE', 'OVERDUE')
                ORDER BY due_at
                """;
        List<Loan> loans = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                loans.add(map(resultSet));
            }
        }
        return loans;
    }

    private Loan map(ResultSet resultSet) throws SQLException {
        Timestamp returnedAt = resultSet.getTimestamp("returned_at");
        return new Loan(
                resultSet.getLong("id"),
                resultSet.getLong("copy_id"),
                resultSet.getLong("member_id"),
                resultSet.getTimestamp("loaned_at").toLocalDateTime(),
                resultSet.getTimestamp("due_at").toLocalDateTime(),
                returnedAt == null ? null : returnedAt.toLocalDateTime(),
                LoanStatus.valueOf(resultSet.getString("status"))
        );
    }
}
