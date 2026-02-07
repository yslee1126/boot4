package com.example.boot4.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "subscriptions")
@Getter
@NoArgsConstructor
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String description;

    @ManyToMany(mappedBy = "subscriptions")
    private List<User> users = new ArrayList<>();

    public Subscription(String name, String description) {
        this.name = name;
        this.description = description;
    }
}
