package ru.library.management.model;

import ru.library.management.model.enums.CopyStatus;

import java.time.LocalDate;

public record BookCopy(
        long id,
        long bookId,
        String inventoryNumber,
        CopyStatus status,
        LocalDate acquiredDate
) {
    public BookCopy {
        if (id < 0 || bookId <= 0) {
            throw new IllegalArgumentException("Invalid copy or book id");
        }
        if (inventoryNumber == null || inventoryNumber.isBlank()) {
            throw new IllegalArgumentException("Inventory number is required");
        }
        if (status == null) {
            throw new IllegalArgumentException("Copy status is required");
        }
        inventoryNumber = inventoryNumber.trim();
    }

    public BookCopy withStatus(CopyStatus newStatus) {
        return new BookCopy(id, bookId, inventoryNumber, newStatus, acquiredDate);
    }
}
