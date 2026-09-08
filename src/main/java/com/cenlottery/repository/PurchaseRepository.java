package com.cenlottery.repository;

import com.cenlottery.domain.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PurchaseRepository extends JpaRepository<Purchase, String> {
    Optional<Purchase> findByIdempotencyKey(String idempotencyKey);

    List<Purchase> findByUserIdOrderByCreatedAtDesc(String userId);
}
