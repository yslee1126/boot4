package com.example.boot4.repository;

import com.example.boot4.domain.User;
import java.util.List;

public interface UserRepositoryCustom {
    List<User> findAllQueryDsl();

    User findByName(String name);
}
