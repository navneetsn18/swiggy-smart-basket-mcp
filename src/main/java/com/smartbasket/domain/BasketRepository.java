package com.smartbasket.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BasketRepository extends JpaRepository<Basket, String> {

    Optional<Basket> findByUserIdAndName(String userId, String name);

    List<Basket> findByUserId(String userId);

    boolean existsByUserIdAndName(String userId, String name);
}
