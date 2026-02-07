package com.example.boot4.repository;

import com.example.boot4.domain.User;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.example.boot4.domain.QUser.user;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<User> findAllQueryDsl() {
        return queryFactory.selectFrom(user)
                .fetch();
    }

    @Override
    public User findByName(String name) {
        return queryFactory.selectFrom(user)
                .where(user.name.eq(name))
                .fetchOne();
    }
}
