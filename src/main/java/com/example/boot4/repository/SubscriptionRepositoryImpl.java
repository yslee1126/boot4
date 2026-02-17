package com.example.boot4.repository;

import com.example.boot4.domain.Subscription;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.example.boot4.domain.QSubscription.subscription;
import static com.example.boot4.domain.QUser.user;

@Repository
@RequiredArgsConstructor
public class SubscriptionRepositoryImpl implements SubscriptionRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Subscription> findSubscriptionsByUserId(Long userId) {
        return queryFactory.selectFrom(subscription)
                .join(subscription.users, user)
                .where(user.id.eq(userId))
                .fetch();
    }
}
