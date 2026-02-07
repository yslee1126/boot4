package com.example.boot4.service;

import com.example.boot4.domain.Subscription;
import com.example.boot4.domain.User;
import com.example.boot4.repository.SubscriptionRepository;
import com.example.boot4.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionService {

    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;

    /**
     * 사용자에게 구독 추가
     */
    @Transactional
    public void addSubscriptionToUser(Long userId, Long subscriptionId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found: " + subscriptionId));

        user.addSubscription(subscription);
        log.info("Added subscription {} to user {}", subscriptionId, userId);
    }

    /**
     * 사용자의 모든 구독 조회 (JOIN 쿼리 발생)
     */
    public List<Subscription> getUserSubscriptions(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        // 이 부분에서 N+1 문제가 발생할 수 있음 (지연 로딩)
        List<Subscription> subscriptions = user.getSubscriptions();
        log.info("User {} has {} subscriptions", userId, subscriptions.size());

        return subscriptions;
    }

    /**
     * 구독 생성
     */
    @Transactional
    public Subscription createSubscription(String name, String description) {
        Subscription subscription = new Subscription(name, description);
        return subscriptionRepository.save(subscription);
    }

    /**
     * 모든 구독 조회
     */
    public List<Subscription> getAllSubscriptions() {
        return subscriptionRepository.findAll();
    }
}
