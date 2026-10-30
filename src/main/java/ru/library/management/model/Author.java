package ru.library.management.model;

public record Author(long id, String fullName) {
    public Author {
        if (id < 0) {
            throw new IllegalArgumentException("Author id cannot be negative");
        }
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Author name is required");
        }
        fullName = fullName.trim();
    }
}
