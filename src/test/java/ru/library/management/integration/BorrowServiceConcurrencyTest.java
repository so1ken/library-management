package ru.library.management.integration;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import ru.library.management.exception.CopyNotAvailableException;
import ru.library.management.infrastructure.TransactionManager;
import ru.library.management.model.Book;
import ru.library.management.model.BookCopy;
import ru.library.management.model.Member;
import ru.library.management.repository.jdbc.JdbcBookCopyRepository;
import ru.library.management.repository.jdbc.JdbcBookRepository;
import ru.library.management.repository.jdbc.JdbcLoanRepository;
import ru.library.management.repository.jdbc.JdbcMemberRepository;
import ru.library.management.service.BorrowService;
import ru.library.management.service.CatalogService;
import ru.library.management.service.MemberService;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BorrowServiceConcurrencyTest {
    private static HikariDataSource dataSource;
    private static CatalogService catalogService;
    private static MemberService memberService;
    private static BorrowService borrowService;

    @BeforeAll
    static void setUp() {
        String url = System.getenv("DB_TEST_URL");
        Assumptions.assumeTrue(url != null && !url.isBlank(), "Set DB_TEST_URL to run PostgreSQL integration tests");

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(environment("DB_TEST_USER", "library_user"));
        config.setPassword(environment("DB_TEST_PASSWORD", "library_password"));
        config.setMaximumPoolSize(5);
        dataSource = new HikariDataSource(config);
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();

        TransactionManager transactionManager = new TransactionManager(dataSource);
        JdbcBookRepository bookRepository = new JdbcBookRepository();
        JdbcBookCopyRepository copyRepository = new JdbcBookCopyRepository();
        JdbcMemberRepository memberRepository = new JdbcMemberRepository();

        catalogService = new CatalogService(transactionManager, bookRepository, copyRepository);
        memberService = new MemberService(transactionManager, memberRepository);
        borrowService = new BorrowService(
                transactionManager, copyRepository, new JdbcLoanRepository(), memberRepository
        );
    }

    @AfterAll
    static void tearDown() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    @Test
    void onlyOneMemberCanBorrowTheSamePhysicalCopy() throws Exception {
        String unique = UUID.randomUUID().toString().substring(0, 12);
        Book book = catalogService.addNewBook("T-" + unique, "Concurrency test " + unique, null, null, List.of());
        BookCopy copy = catalogService.addCopy(book.id(), "INV-" + unique, null);
        Member firstMember = memberService.registerMember("Test Member A " + unique, null, null);
        Member secondMember = memberService.registerMember("Test Member B " + unique, null, null);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> first = executor.submit(() -> tryBorrow(firstMember.id(), copy.id(), ready, start));
            Future<Boolean> second = executor.submit(() -> tryBorrow(secondMember.id(), copy.id(), ready, start));

            ready.await(5, TimeUnit.SECONDS);
            start.countDown();

            int successfulBorrows = (first.get(10, TimeUnit.SECONDS) ? 1 : 0)
                    + (second.get(10, TimeUnit.SECONDS) ? 1 : 0);
            assertEquals(1, successfulBorrows);
        } finally {
            executor.shutdownNow();
        }
    }

    private boolean tryBorrow(long memberId, long copyId, CountDownLatch ready, CountDownLatch start)
            throws InterruptedException {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Test start timed out");
        }
        try {
            borrowService.borrowBook(memberId, copyId);
            return true;
        } catch (CopyNotAvailableException exception) {
            return false;
        }
    }

    private static String environment(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
