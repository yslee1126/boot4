package com.example.boot4.repository;

import com.example.boot4.domain.Subscription;
import java.util.List;

public interface SubscriptionRepositoryCustom {
    List<Subscription> findSubscriptionsByUserId(Long userId);
}
