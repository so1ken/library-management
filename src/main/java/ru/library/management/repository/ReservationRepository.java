package ru.library.management.repository;

import ru.library.management.model.Reservation;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface ReservationRepository {
    Reservation save(Connection connection, Reservation reservation) throws SQLException;

    boolean cancel(Connection connection, long reservationId) throws SQLException;

    List<Reservation> findByMemberId(Connection connection, long memberId) throws SQLException;
}
