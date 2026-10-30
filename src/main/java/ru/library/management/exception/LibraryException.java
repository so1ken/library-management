package ru.library.management.exception;

public class LibraryException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    public LibraryException(String message) {
        super(message);
    }

    public LibraryException(String message, Throwable cause) {
        super(message, cause);
    }
}
