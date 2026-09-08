package com.cenlottery.repository;

import com.cenlottery.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByUsernameLower(String usernameLower);

    boolean existsByUsernameLower(String usernameLower);
}
