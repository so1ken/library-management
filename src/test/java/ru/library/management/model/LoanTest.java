package ru.library.management.model;

import org.junit.jupiter.api.Test;
import ru.library.management.model.enums.LoanStatus;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LoanTest {
    @Test
    void activeLoanMustNotHaveReturnedAt() {
        LocalDateTime now = LocalDateTime.now();
        assertDoesNotThrow(() -> new Loan(0, 1, 1, now, now.plusDays(14), null, LoanStatus.ACTIVE));
        assertThrows(IllegalArgumentException.class,
                () -> new Loan(0, 1, 1, now, now.plusDays(14), now, LoanStatus.ACTIVE));
    }

    @Test
    void returnedLoanMustHaveReturnedAt() {
        LocalDateTime now = LocalDateTime.now();
        assertDoesNotThrow(() -> new Loan(1, 1, 1, now, now.plusDays(14), now.plusDays(2), LoanStatus.RETURNED));
        assertThrows(IllegalArgumentException.class,
                () -> new Loan(1, 1, 1, now, now.plusDays(14), null, LoanStatus.RETURNED));
    }
}
