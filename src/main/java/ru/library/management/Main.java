package ru.library.management;

import ru.library.management.config.DatabaseConnection;
import ru.library.management.infrastructure.FlywayMigration;
import ru.library.management.infrastructure.TransactionManager;
import ru.library.management.repository.jdbc.JdbcBookCopyRepository;
import ru.library.management.repository.jdbc.JdbcBookRepository;
import ru.library.management.repository.jdbc.JdbcLoanRepository;
import ru.library.management.repository.jdbc.JdbcMemberRepository;
import ru.library.management.repository.jdbc.JdbcReservationRepository;
import ru.library.management.service.BorrowService;
import ru.library.management.service.CatalogService;
import ru.library.management.service.MemberService;
import ru.library.management.service.ReservationService;
import ru.library.management.ui.ConsoleUI;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        try (DatabaseConnection databaseConnection = new DatabaseConnection()) {
            FlywayMigration.migrate(databaseConnection.getDataSource());

            TransactionManager transactionManager = new TransactionManager(databaseConnection.getDataSource());

            JdbcBookRepository bookRepository = new JdbcBookRepository();
            JdbcBookCopyRepository bookCopyRepository = new JdbcBookCopyRepository();
            JdbcMemberRepository memberRepository = new JdbcMemberRepository();
            JdbcLoanRepository loanRepository = new JdbcLoanRepository();
            JdbcReservationRepository reservationRepository = new JdbcReservationRepository();

            BorrowService borrowService = new BorrowService(
                    transactionManager, bookCopyRepository, loanRepository, memberRepository
            );
            ReservationService reservationService = new ReservationService(
                    transactionManager, reservationRepository, bookRepository, bookCopyRepository, memberRepository
            );
            CatalogService catalogService = new CatalogService(
                    transactionManager, bookRepository, bookCopyRepository
            );
            MemberService memberService = new MemberService(transactionManager, memberRepository);

            new ConsoleUI(borrowService, reservationService, catalogService, memberService).start();
        } catch (Exception exception) {
            System.err.println("Не удалось запустить приложение: " + exception.getMessage());
            System.err.println("Проверьте, что PostgreSQL запущен и параметры DB_URL / DB_USER / DB_PASSWORD корректны.");
        }
    }
}
