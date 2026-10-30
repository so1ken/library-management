package ru.library.management.service;

import ru.library.management.infrastructure.TransactionManager;
import ru.library.management.model.Member;
import ru.library.management.model.enums.MemberStatus;
import ru.library.management.repository.MemberRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class MemberService {
    private final TransactionManager transactionManager;
    private final MemberRepository memberRepository;

    public MemberService(TransactionManager transactionManager, MemberRepository memberRepository) {
        this.transactionManager = transactionManager;
        this.memberRepository = memberRepository;
    }

    public Member registerMember(String fullName, LocalDate birthDate, String email) {
        return transactionManager.inTransaction(connection -> {
            Member draft = new Member(0, fullName, birthDate, email, MemberStatus.ACTIVE, LocalDateTime.now());
            return memberRepository.save(connection, draft);
        });
    }

    public List<Member> getMembers() {
        return transactionManager.inTransaction(memberRepository::findAll);
    }
}
