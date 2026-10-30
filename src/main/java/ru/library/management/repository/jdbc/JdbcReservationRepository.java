package ru.library.management.repository.jdbc;

import ru.library.management.model.Reservation;
import ru.library.management.model.enums.ReservationStatus;
import ru.library.management.repository.ReservationRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public final class JdbcReservationRepository implements ReservationRepository {
    @Override
    public Reservation save(Connection connection, Reservation reservation) throws SQLException {
        String sql = """
                INSERT INTO reservations (book_id, member_id, copy_id, created_at, expires_at, status)
                VALUES (?, ?, ?, ?, ?, ?)
                RETURNING id
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, reservation.bookId());
            statement.setLong(2, reservation.memberId());
            if (reservation.copyId() == null) {
                statement.setNull(3, java.sql.Types.BIGINT);
            } else {
                statement.setLong(3, reservation.copyId());
            }
            statement.setTimestamp(4, Timestamp.valueOf(reservation.createdAt()));
            statement.setTimestamp(5, Timestamp.valueOf(reservation.expiresAt()));
            statement.setString(6, reservation.status().name());
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return new Reservation(
                        resultSet.getLong("id"), reservation.bookId(), reservation.memberId(),
                        reservation.copyId(), reservation.createdAt(), reservation.expiresAt(),
                        reservation.status()
                );
            }
        }
    }

    @Override
    public boolean cancel(Connection connection, long reservationId) throws SQLException {
        String sql = """
                UPDATE reservations
                SET status = 'CANCELLED'
                WHERE id = ? AND status IN ('PENDING', 'READY')
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, reservationId);
            return statement.executeUpdate() == 1;
        }
    }

    @Override
    public List<Reservation> findByMemberId(Connection connection, long memberId) throws SQLException {
        String sql = """
                SELECT id, book_id, member_id, copy_id, created_at, expires_at, status
                FROM reservations
                WHERE member_id = ?
                ORDER BY created_at DESC
                """;
        List<Reservation> reservations = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, memberId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    reservations.add(map(resultSet));
                }
            }
        }
        return reservations;
    }

    private Reservation map(ResultSet resultSet) throws SQLException {
        long copyIdValue = resultSet.getLong("copy_id");
        Long copyId = resultSet.wasNull() ? null : copyIdValue;
        return new Reservation(
                resultSet.getLong("id"),
                resultSet.getLong("book_id"),
                resultSet.getLong("member_id"),
                copyId,
                resultSet.getTimestamp("created_at").toLocalDateTime(),
                resultSet.getTimestamp("expires_at").toLocalDateTime(),
                ReservationStatus.valueOf(resultSet.getString("status"))
        );
    }
}
