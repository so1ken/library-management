package ru.library.management.service;

import ru.library.management.exception.CopyNotAvailableException;
import ru.library.management.exception.EntityNotFoundException;
import ru.library.management.exception.MemberBlockedException;
import ru.library.management.infrastructure.TransactionManager;
import ru.library.management.model.BookCopy;
import ru.library.management.model.Loan;
import ru.library.management.model.Member;
import ru.library.management.model.enums.CopyStatus;
import ru.library.management.model.enums.LoanStatus;
import ru.library.management.model.enums.MemberStatus;
import ru.library.management.repository.BookCopyRepository;
import ru.library.management.repository.LoanRepository;
import ru.library.management.repository.MemberRepository;

import java.time.LocalDateTime;
import java.util.List;

public final class BorrowService {
    private static final int LOAN_PERIOD_DAYS = 14;

    private final TransactionManager transactionManager;
    private final BookCopyRepository bookCopyRepository;
    private final LoanRepository loanRepository;
    private final MemberRepository memberRepository;

    public BorrowService(
            TransactionManager transactionManager,
            BookCopyRepository bookCopyRepository,
            LoanRepository loanRepository,
            MemberRepository memberRepository
    ) {
        this.transactionManager = transactionManager;
        this.bookCopyRepository = bookCopyRepository;
        this.loanRepository = loanRepository;
        this.memberRepository = memberRepository;
    }

    public Loan borrowBook(long memberId, long copyId) {
        return transactionManager.inTransaction(connection -> {
            Member member = memberRepository.findById(connection, memberId)
                    .orElseThrow(() -> new EntityNotFoundException("Читатель с id " + memberId + " не найден"));
            ensureMemberCanBorrow(member);

            BookCopy copy = bookCopyRepository.findByIdForUpdate(connection, copyId)
                    .orElseThrow(() -> new EntityNotFoundException("Экземпляр с id " + copyId + " не найден"));
            if (copy.status() != CopyStatus.AVAILABLE) {
                throw new CopyNotAvailableException(
                        "Экземпляр " + copy.inventoryNumber() + " недоступен: " + copy.status()
                );
            }

            LocalDateTime now = LocalDateTime.now();
            Loan newLoan = new Loan(
                    0,
                    copyId,
                    memberId,
                    now,
                    now.plusDays(LOAN_PERIOD_DAYS),
                    null,
                    LoanStatus.ACTIVE
            );

            Loan savedLoan = loanRepository.save(connection, newLoan);
            bookCopyRepository.updateStatus(connection, copyId, CopyStatus.LOANED);
            return savedLoan;
        });
    }

    public void returnBook(long copyId) {
        transactionManager.inTransaction(connection -> {
            BookCopy copy = bookCopyRepository.findByIdForUpdate(connection, copyId)
                    .orElseThrow(() -> new EntityNotFoundException("Экземпляр с id " + copyId + " не найден"));
            if (copy.status() != CopyStatus.LOANED) {
                throw new CopyNotAvailableException("Экземпляр не числится выданным");
            }

            Loan loan = loanRepository.findActiveByCopyId(connection, copyId)
                    .orElseThrow(() -> new EntityNotFoundException("Активная выдача для экземпляра не найдена"));
            loanRepository.markReturned(connection, loan.id(), LocalDateTime.now());
            bookCopyRepository.updateStatus(connection, copyId, CopyStatus.AVAILABLE);
            return null;
        });
    }

    public List<Loan> getActiveLoans() {
        return transactionManager.inTransaction(connection -> {
            loanRepository.markOverdueLoans(connection, LocalDateTime.now());
            return loanRepository.findActiveLoans(connection);
        });
    }

    private void ensureMemberCanBorrow(Member member) {
        if (member.status() != MemberStatus.ACTIVE) {
            throw new MemberBlockedException("Читатель заблокирован и не может получать книги");
        }
    }
}
