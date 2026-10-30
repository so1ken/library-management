package ru.library.management.model;

import java.util.List;

public record Book(
        long id,
        String isbn,
        String title,
        String publisher,
        Integer year,
        List<Author> authors
) {
    public Book {
        if (id < 0) {
            throw new IllegalArgumentException("Book id cannot be negative");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Book title is required");
        }
        title = title.trim();
        isbn = normalize(isbn);
        publisher = normalize(publisher);
        authors = authors == null ? List.of() : List.copyOf(authors);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
