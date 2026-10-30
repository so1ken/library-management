package ru.library.management.exception;

public class CopyNotAvailableException extends LibraryException {
    private static final long serialVersionUID = 1L;
    public CopyNotAvailableException(String message) {
        super(message);
    }
}
