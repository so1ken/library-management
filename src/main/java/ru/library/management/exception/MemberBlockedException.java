package ru.library.management.exception;

public class MemberBlockedException extends LibraryException {
    private static final long serialVersionUID = 1L;
    public MemberBlockedException(String message) {
        super(message);
    }
}
