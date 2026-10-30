package ru.library.management.exception;

public class DataAccessException extends LibraryException {
    private static final long serialVersionUID = 1L;
    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
