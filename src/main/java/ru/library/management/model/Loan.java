package ru.library.management.model;

import ru.library.management.model.enums.LoanStatus;

import java.time.LocalDateTime;

public record Loan(
        long id,
        long copyId,
        long memberId,
        LocalDateTime loanedAt,
        LocalDateTime dueAt,
        LocalDateTime returnedAt,
        LoanStatus status
) {
    public Loan {
        if (id < 0 || copyId <= 0 || memberId <= 0) {
            throw new IllegalArgumentException("Invalid loan, copy or member id");
        }
        if (loanedAt == null || dueAt == null || status == null || !dueAt.isAfter(loanedAt)) {
            throw new IllegalArgumentException("Invalid loan dates or status");
        }
        if ((status == LoanStatus.RETURNED) != (returnedAt != null)) {
            throw new IllegalArgumentException("Returned loans must have returnedAt; active loans must not");
        }
    }
}
