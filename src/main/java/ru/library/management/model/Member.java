package ru.library.management.model;

import ru.library.management.model.enums.MemberStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record Member(
        long id,
        String fullName,
        LocalDate birthDate,
        String email,
        MemberStatus status,
        LocalDateTime registeredAt
) {
    public Member {
        if (id < 0) {
            throw new IllegalArgumentException("Member id cannot be negative");
        }
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Member name is required");
        }
        if (status == null || registeredAt == null) {
            throw new IllegalArgumentException("Member status and registration date are required");
        }
        fullName = fullName.trim();
        email = email == null || email.isBlank() ? null : email.trim();
    }
}
