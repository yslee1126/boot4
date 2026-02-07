package com.example.boot4.controller;

import com.example.boot4.domain.Subscription;
import com.example.boot4.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    /**
     * 구독 생성
     */
    @PostMapping
    public ResponseEntity<Subscription> createSubscription(@RequestBody Map<String, String> request) {
        String name = request.get("name");
        String description = request.get("description");
        Subscription subscription = subscriptionService.createSubscription(name, description);
        return ResponseEntity.ok(subscription);
    }

    /**
     * 모든 구독 조회
     */
    @GetMapping
    public ResponseEntity<List<Subscription>> getAllSubscriptions() {
        return ResponseEntity.ok(subscriptionService.getAllSubscriptions());
    }

    /**
     * 사용자에게 구독 추가
     */
    @PostMapping("/users/{userId}/subscriptions/{subscriptionId}")
    public ResponseEntity<Void> addSubscriptionToUser(
            @PathVariable Long userId,
            @PathVariable Long subscriptionId) {
        subscriptionService.addSubscriptionToUser(userId, subscriptionId);
        return ResponseEntity.ok().build();
    }

    /**
     * 사용자의 구독 목록 조회
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<List<Subscription>> getUserSubscriptions(@PathVariable Long userId) {
        return ResponseEntity.ok(subscriptionService.getUserSubscriptions(userId));
    }
}
