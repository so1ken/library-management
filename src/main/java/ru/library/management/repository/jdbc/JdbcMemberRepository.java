package ru.library.management.repository.jdbc;

import ru.library.management.model.Member;
import ru.library.management.model.enums.MemberStatus;
import ru.library.management.repository.MemberRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class JdbcMemberRepository implements MemberRepository {
    @Override
    public Member save(Connection connection, Member member) throws SQLException {
        String sql = """
                INSERT INTO members (full_name, birth_date, email, status)
                VALUES (?, ?, ?, ?)
                RETURNING id, registered_at
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, member.fullName());
            statement.setObject(2, member.birthDate());
            statement.setString(3, member.email());
            statement.setString(4, member.status().name());
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                Timestamp registeredAt = resultSet.getTimestamp("registered_at");
                return new Member(
                        resultSet.getLong("id"), member.fullName(), member.birthDate(), member.email(),
                        member.status(), registeredAt.toLocalDateTime()
                );
            }
        }
    }

    @Override
    public Optional<Member> findById(Connection connection, long id) throws SQLException {
        String sql = """
                SELECT id, full_name, birth_date, email, status, registered_at
                FROM members
                WHERE id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        }
    }

    @Override
    public List<Member> findAll(Connection connection) throws SQLException {
        String sql = """
                SELECT id, full_name, birth_date, email, status, registered_at
                FROM members
                ORDER BY full_name, id
                """;
        List<Member> members = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                members.add(map(resultSet));
            }
        }
        return members;
    }

    private Member map(ResultSet resultSet) throws SQLException {
        Timestamp registeredAt = resultSet.getTimestamp("registered_at");
        return new Member(
                resultSet.getLong("id"),
                resultSet.getString("full_name"),
                resultSet.getObject("birth_date", java.time.LocalDate.class),
                resultSet.getString("email"),
                MemberStatus.valueOf(resultSet.getString("status")),
                registeredAt.toLocalDateTime()
        );
    }
}
