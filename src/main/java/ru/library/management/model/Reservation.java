package ru.library.management.model;

import ru.library.management.model.enums.ReservationStatus;

import java.time.LocalDateTime;

public record Reservation(
        long id,
        long bookId,
        long memberId,
        Long copyId,
        LocalDateTime createdAt,
        LocalDateTime expiresAt,
        ReservationStatus status
) {
    public Reservation {
        if (id < 0 || bookId <= 0 || memberId <= 0 || (copyId != null && copyId <= 0)) {
            throw new IllegalArgumentException("Invalid reservation ids");
        }
        if (createdAt == null || expiresAt == null || status == null || !expiresAt.isAfter(createdAt)) {
            throw new IllegalArgumentException("Invalid reservation dates or status");
        }
    }
}
