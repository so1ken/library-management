package ru.library.management.ui;

import ru.library.management.model.Book;
import ru.library.management.model.BookCopy;
import ru.library.management.model.Loan;
import ru.library.management.model.Member;
import ru.library.management.model.Reservation;
import ru.library.management.service.BorrowService;
import ru.library.management.service.CatalogService;
import ru.library.management.service.MemberService;
import ru.library.management.service.ReservationService;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public final class ConsoleUI {
    private final Scanner scanner;
    private final BorrowService borrowService;
    private final ReservationService reservationService;
    private final CatalogService catalogService;
    private final MemberService memberService;

    public ConsoleUI(
            BorrowService borrowService,
            ReservationService reservationService,
            CatalogService catalogService,
            MemberService memberService
    ) {
        this.scanner = new Scanner(System.in);
        this.borrowService = borrowService;
        this.reservationService = reservationService;
        this.catalogService = catalogService;
        this.memberService = memberService;
    }

    public void start() {
        System.out.println("=== Library Management ===");
        boolean running = true;
        while (running) {
            printMenu();
            String command = scanner.nextLine().trim();
            switch (command) {
                case "1" -> execute(this::showBooks);
                case "2" -> execute(this::addBook);
                case "3" -> execute(this::addCopy);
                case "4" -> execute(this::registerMember);
                case "5" -> execute(this::showCopies);
                case "6" -> execute(this::borrowBook);
                case "7" -> execute(this::returnBook);
                case "8" -> execute(this::createReservation);
                case "9" -> execute(this::cancelReservation);
                case "10" -> execute(this::showMembers);
                case "11" -> execute(this::showActiveLoans);
                case "12" -> execute(this::showMemberReservations);
                case "0" -> running = false;
                default -> System.out.println("Неизвестная команда. Выберите пункт меню от 0 до 12.");
            }
        }
        System.out.println("Работа приложения завершена.");
    }

    private void printMenu() {
        System.out.println("""

                1. Показать каталог книг
                2. Добавить книгу
                3. Добавить экземпляр книги
                4. Зарегистрировать читателя
                5. Показать экземпляры книги
                6. Выдать экземпляр читателю
                7. Вернуть экземпляр
                8. Забронировать книгу
                9. Отменить бронь
                10. Показать читателей
                11. Показать активные выдачи
                12. Показать брони читателя
                0. Выход
                """);
        System.out.print("Выберите действие: ");
    }

    private void showBooks() {
        List<Book> books = catalogService.getBooks();
        if (books.isEmpty()) {
            System.out.println("Каталог пуст.");
            return;
        }
        for (Book book : books) {
            String authors = book.authors().isEmpty()
                    ? "не указаны"
                    : String.join(", ", book.authors().stream().map(author -> author.fullName()).toList());
            System.out.printf(
                    "id=%d | %s | ISBN: %s | Автор(ы): %s | Издательство: %s | Год: %s%n",
                    book.id(), book.title(), valueOrDash(book.isbn()), authors,
                    valueOrDash(book.publisher()), valueOrDash(book.year())
            );
        }
    }

    private void addBook() {
        System.out.println("Добавление книги");
        String isbn = readOptional("ISBN (можно пропустить): ");
        String title = readRequired("Название: ");
        String publisher = readOptional("Издательство (можно пропустить): ");
        String yearInput = readOptional("Год издания (можно пропустить): ");
        Integer year = yearInput == null ? null : parseInteger(yearInput, "Год");
        String authorsInput = readOptional("Авторы через запятую (можно пропустить): ");
        List<String> authors = authorsInput == null
                ? List.of()
                : Arrays.stream(authorsInput.split(",")).map(String::trim).filter(s -> !s.isBlank()).toList();

        Book book = catalogService.addNewBook(isbn, title, publisher, year, authors);
        System.out.println("Книга добавлена. id=" + book.id());
    }

    private void addCopy() {
        long bookId = readLong("ID книги: ");
        String inventoryNumber = readRequired("Инвентарный номер: ");
        LocalDate acquiredDate = readOptionalDate("Дата поступления YYYY-MM-DD (можно пропустить): ");
        BookCopy copy = catalogService.addCopy(bookId, inventoryNumber, acquiredDate);
        System.out.println("Экземпляр добавлен. id=" + copy.id());
    }

    private void registerMember() {
        String fullName = readRequired("ФИО читателя: ");
        LocalDate birthDate = readOptionalDate("Дата рождения YYYY-MM-DD (можно пропустить): ");
        String email = readOptional("Email (можно пропустить): ");
        Member member = memberService.registerMember(fullName, birthDate, email);
        System.out.println("Читатель зарегистрирован. id=" + member.id());
    }

    private void showCopies() {
        long bookId = readLong("ID книги: ");
        List<BookCopy> copies = catalogService.getCopies(bookId);
        if (copies.isEmpty()) {
            System.out.println("У этой книги пока нет физических экземпляров.");
            return;
        }
        copies.forEach(copy -> System.out.printf(
                "id=%d | инв. № %s | статус: %s | дата поступления: %s%n",
                copy.id(), copy.inventoryNumber(), copy.status(), valueOrDash(copy.acquiredDate())
        ));
    }

    private void borrowBook() {
        long memberId = readLong("ID читателя: ");
        long copyId = readLong("ID экземпляра: ");
        Loan loan = borrowService.borrowBook(memberId, copyId);
        System.out.printf("Выдача оформлена. ID=%d, вернуть до %s%n", loan.id(), loan.dueAt());
    }

    private void returnBook() {
        long copyId = readLong("ID экземпляра: ");
        borrowService.returnBook(copyId);
        System.out.println("Книга успешно возвращена.");
    }

    private void createReservation() {
        long memberId = readLong("ID читателя: ");
        long bookId = readLong("ID книги: ");
        Reservation reservation = reservationService.createReservation(memberId, bookId);
        System.out.println("Бронь создана. ID=" + reservation.id() + ", действует до " + reservation.expiresAt());
    }

    private void cancelReservation() {
        long reservationId = readLong("ID брони: ");
        reservationService.cancelReservation(reservationId);
        System.out.println("Бронь отменена.");
    }

    private void showMembers() {
        List<Member> members = memberService.getMembers();
        if (members.isEmpty()) {
            System.out.println("Читателей пока нет.");
            return;
        }
        members.forEach(member -> System.out.printf(
                "id=%d | %s | email: %s | статус: %s | регистрация: %s%n",
                member.id(), member.fullName(), valueOrDash(member.email()), member.status(), member.registeredAt()
        ));
    }

    private void showActiveLoans() {
        List<Loan> loans = borrowService.getActiveLoans();
        if (loans.isEmpty()) {
            System.out.println("Активных выдач нет.");
            return;
        }
        loans.forEach(loan -> System.out.printf(
                "id=%d | экземпляр=%d | читатель=%d | выдана=%s | вернуть до=%s | статус=%s%n",
                loan.id(), loan.copyId(), loan.memberId(), loan.loanedAt(), loan.dueAt(), loan.status()
        ));
    }

    private void showMemberReservations() {
        long memberId = readLong("ID читателя: ");
        List<Reservation> reservations = reservationService.getReservationsForMember(memberId);
        if (reservations.isEmpty()) {
            System.out.println("У читателя нет броней.");
            return;
        }
        reservations.forEach(reservation -> System.out.printf(
                "id=%d | книга=%d | экземпляр=%s | создана=%s | истекает=%s | статус=%s%n",
                reservation.id(), reservation.bookId(), valueOrDash(reservation.copyId()),
                reservation.createdAt(), reservation.expiresAt(), reservation.status()
        ));
    }

    private void execute(Runnable action) {
        try {
            action.run();
        } catch (Exception exception) {
            String message = exception.getMessage();
            System.out.println("Ошибка: " + (message == null ? exception.getClass().getSimpleName() : message));
        }
    }

    private String readRequired(String prompt) {
        while (true) {
            System.out.print(prompt);
            String value = scanner.nextLine().trim();
            if (!value.isBlank()) {
                return value;
            }
            System.out.println("Значение обязательно.");
        }
    }

    private String readOptional(String prompt) {
        System.out.print(prompt);
        String value = scanner.nextLine().trim();
        return value.isBlank() ? null : value;
    }

    private long readLong(String prompt) {
        String value = readRequired(prompt);
        try {
            long parsed = Long.parseLong(value);
            if (parsed <= 0) {
                throw new NumberFormatException();
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Введите положительное целое число");
        }
    }

    private Integer parseInteger(String value, String fieldName) {
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(fieldName + " должен быть целым числом");
        }
    }

    private LocalDate readOptionalDate(String prompt) {
        String value = readOptional(prompt);
        if (value == null) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Неверный формат даты. Используйте YYYY-MM-DD");
        }
    }

    private String valueOrDash(Object value) {
        return value == null ? "—" : value.toString();
    }
}
