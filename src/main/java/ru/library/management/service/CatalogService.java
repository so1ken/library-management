package ru.library.management.service;

import ru.library.management.exception.EntityNotFoundException;
import ru.library.management.infrastructure.TransactionManager;
import ru.library.management.model.Book;
import ru.library.management.model.BookCopy;
import ru.library.management.model.enums.CopyStatus;
import ru.library.management.repository.BookCopyRepository;
import ru.library.management.repository.BookRepository;

import java.time.LocalDate;
import java.util.List;

public final class CatalogService {
    private final TransactionManager transactionManager;
    private final BookRepository bookRepository;
    private final BookCopyRepository bookCopyRepository;

    public CatalogService(
            TransactionManager transactionManager,
            BookRepository bookRepository,
            BookCopyRepository bookCopyRepository
    ) {
        this.transactionManager = transactionManager;
        this.bookRepository = bookRepository;
        this.bookCopyRepository = bookCopyRepository;
    }

    public Book addNewBook(String isbn, String title, String publisher, Integer year, List<String> authors) {
        return transactionManager.inTransaction(connection -> {
            Book draft = new Book(0, isbn, title, publisher, year, List.of());
            return bookRepository.save(connection, draft, authors);
        });
    }

    public BookCopy addCopy(long bookId, String inventoryNumber, LocalDate acquiredDate) {
        return transactionManager.inTransaction(connection -> {
            bookRepository.findById(connection, bookId)
                    .orElseThrow(() -> new EntityNotFoundException("Книга с id " + bookId + " не найдена"));
            BookCopy copy = new BookCopy(0, bookId, inventoryNumber, CopyStatus.AVAILABLE, acquiredDate);
            return bookCopyRepository.save(connection, copy);
        });
    }

    public List<Book> getBooks() {
        return transactionManager.inTransaction(bookRepository::findAll);
    }

    public List<BookCopy> getCopies(long bookId) {
        return transactionManager.inTransaction(connection -> {
            bookRepository.findById(connection, bookId)
                    .orElseThrow(() -> new EntityNotFoundException("Книга с id " + bookId + " не найдена"));
            return bookCopyRepository.findByBookId(connection, bookId);
        });
    }
}
