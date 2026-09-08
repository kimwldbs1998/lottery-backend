package com.cenlottery.repository;

import com.cenlottery.domain.Round;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoundRepository extends JpaRepository<Round, Long> {
    Optional<Round> findFirstByStatusOrderByRoundNumberDesc(String status);

    Page<Round> findAllByOrderByRoundNumberDesc(Pageable pageable);
}
