package ru.library.management.repository;

import ru.library.management.model.Member;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface MemberRepository {
    Member save(Connection connection, Member member) throws SQLException;

    Optional<Member> findById(Connection connection, long id) throws SQLException;

    List<Member> findAll(Connection connection) throws SQLException;
}
