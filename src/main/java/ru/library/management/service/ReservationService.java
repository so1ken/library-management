package ru.library.management.service;

import ru.library.management.exception.EntityNotFoundException;
import ru.library.management.exception.LibraryException;
import ru.library.management.exception.MemberBlockedException;
import ru.library.management.infrastructure.TransactionManager;
import ru.library.management.model.Member;
import ru.library.management.model.Reservation;
import ru.library.management.model.enums.MemberStatus;
import ru.library.management.model.enums.ReservationStatus;
import ru.library.management.repository.BookCopyRepository;
import ru.library.management.repository.BookRepository;
import ru.library.management.repository.MemberRepository;
import ru.library.management.repository.ReservationRepository;

import java.time.LocalDateTime;
import java.util.List;

public final class ReservationService {
    private static final int RESERVATION_PERIOD_DAYS = 7;

    private final TransactionManager transactionManager;
    private final ReservationRepository reservationRepository;
    private final BookRepository bookRepository;
    private final BookCopyRepository bookCopyRepository;
    private final MemberRepository memberRepository;

    public ReservationService(
            TransactionManager transactionManager,
            ReservationRepository reservationRepository,
            BookRepository bookRepository,
            BookCopyRepository bookCopyRepository,
            MemberRepository memberRepository
    ) {
        this.transactionManager = transactionManager;
        this.reservationRepository = reservationRepository;
        this.bookRepository = bookRepository;
        this.bookCopyRepository = bookCopyRepository;
        this.memberRepository = memberRepository;
    }

    public Reservation createReservation(long memberId, long bookId) {
        return transactionManager.inTransaction(connection -> {
            Member member = memberRepository.findById(connection, memberId)
                    .orElseThrow(() -> new EntityNotFoundException("Читатель с id " + memberId + " не найден"));
            if (member.status() != MemberStatus.ACTIVE) {
                throw new MemberBlockedException("Заблокированный читатель не может бронировать книги");
            }
            bookRepository.findById(connection, bookId)
                    .orElseThrow(() -> new EntityNotFoundException("Книга с id " + bookId + " не найдена"));

            if (bookCopyRepository.countAvailableByBookId(connection, bookId) > 0) {
                throw new LibraryException("У книги есть доступные экземпляры. Оформите обычную выдачу");
            }

            LocalDateTime now = LocalDateTime.now();
            Reservation reservation = new Reservation(
                    0,
                    bookId,
                    memberId,
                    null,
                    now,
                    now.plusDays(RESERVATION_PERIOD_DAYS),
                    ReservationStatus.PENDING
            );
            return reservationRepository.save(connection, reservation);
        });
    }

    public void cancelReservation(long reservationId) {
        transactionManager.inTransaction(connection -> {
            if (!reservationRepository.cancel(connection, reservationId)) {
                throw new LibraryException(
                        "Бронь не найдена или уже имеет статус, при котором отмена невозможна"
                );
            }
            return null;
        });
    }

    public List<Reservation> getReservationsForMember(long memberId) {
        return transactionManager.inTransaction(connection -> {
            memberRepository.findById(connection, memberId)
                    .orElseThrow(() -> new EntityNotFoundException("Читатель с id " + memberId + " не найден"));
            return reservationRepository.findByMemberId(connection, memberId);
        });
    }
}
