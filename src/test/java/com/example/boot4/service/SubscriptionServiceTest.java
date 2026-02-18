package com.example.boot4.service;

import com.example.boot4.domain.Subscription;
import com.example.boot4.domain.User;
import com.example.boot4.repository.SubscriptionRepository;
import com.example.boot4.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/*
  실행 방법 
  JASYPT_KEY="your_key" ./gradlew cleanTest test --tests SubscriptionServiceTest.testGetUserSubscriptions -Dspring.profiles.active=your_profile --info
 */
@Slf4j
@SpringBootTest
@Transactional
class SubscriptionServiceTest {

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    private User testUser;
    private Subscription testSubscription1;
    private Subscription testSubscription2;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 생성
        testUser = new User("TestUser");
        userRepository.save(testUser);

        testSubscription1 = new Subscription("Premium Plan", "Premium subscription with all features");
        testSubscription2 = new Subscription("Basic Plan", "Basic subscription with limited features");
        subscriptionRepository.save(testSubscription1);
        subscriptionRepository.save(testSubscription2);

        log.info("Test data created: User({}), Subscriptions({}, {})",
                testUser.getId(), testSubscription1.getId(), testSubscription2.getId());
    }

    @Test
    void testAddSubscriptionToUser() {
        // Given
        Long userId = testUser.getId();
        Long subscriptionId = testSubscription1.getId();

        // When
        subscriptionService.addSubscriptionToUser(userId, subscriptionId);

        // Then
        User user = userRepository.findById(userId).orElseThrow();
        assertThat(user.getSubscriptions()).hasSize(1);
        assertThat(user.getSubscriptions().get(0).getName()).isEqualTo("Premium Plan");

        log.info("✅ Successfully added subscription to user");
    }

    @Test
    void testGetUserSubscriptions() {
        // Given: 사용자에게 2개의 구독 추가
        subscriptionService.addSubscriptionToUser(testUser.getId(), testSubscription1.getId());
        subscriptionService.addSubscriptionToUser(testUser.getId(), testSubscription2.getId());

        log.info("🔍 Starting query execution to fetch user subscriptions...");
        log.info("Expected SQL: SELECT with JOIN between users and subscriptions");

        // When: 사용자의 구독 조회 (JOIN 쿼리 발생)
        List<Subscription> subscriptions = subscriptionService.getUserSubscriptions(testUser.getId());

        // Then
        assertThat(subscriptions).hasSize(2);
        assertThat(subscriptions)
                .extracting(Subscription::getName)
                .containsExactlyInAnyOrder("Premium Plan", "Basic Plan");

        log.info("✅ Query executed successfully. Retrieved {} subscriptions", subscriptions.size());
        log.info("📊 This query should show JOIN between user_subscriptions and subscriptions tables");
    }

    @Test
    void testGetAllSubscriptions() {
        // When
        List<Subscription> subscriptions = subscriptionService.getAllSubscriptions();

        // Then
        assertThat(subscriptions).hasSizeGreaterThanOrEqualTo(2);

        log.info("Retrieved all subscriptions: {}", subscriptions.size());
    }

    /**
     * 이 테스트에서 발생하는 주요 쿼리:
     *
     * 1. SELECT FROM users WHERE id = ?
     * 2. SELECT FROM user_subscriptions WHERE user_id = ?
     * 3. SELECT FROM subscriptions WHERE id IN (?, ?)
     *
     * MCP 도구로 위 쿼리들의 실행 계획을 확인할 수 있습니다.
     */
    @Test
    void demonstrateQueryForMCP() {
        // Given
        subscriptionService.addSubscriptionToUser(testUser.getId(), testSubscription1.getId());
        subscriptionService.addSubscriptionToUser(testUser.getId(), testSubscription2.getId());

        log.info("=".repeat(60));
        log.info("🎯 MCP Test: Executing query to fetch user with subscriptions");
        log.info("=".repeat(60));

        // When: 이 부분에서 JOIN 쿼리 발생
        List<Subscription> subscriptions = subscriptionService.getUserSubscriptions(testUser.getId());

        log.info("📝 Expected Query Pattern:");
        log.info("   SELECT * FROM users WHERE id = {}", testUser.getId());
        log.info("   SELECT * FROM user_subscriptions WHERE user_id = {}", testUser.getId());
        log.info("   SELECT * FROM subscriptions WHERE id IN (...)");
        log.info("=".repeat(60));

        // Then
        assertThat(subscriptions).isNotEmpty();
    }
}
