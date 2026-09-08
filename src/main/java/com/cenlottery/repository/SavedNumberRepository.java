package com.cenlottery.repository;

import com.cenlottery.domain.SavedNumber;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SavedNumberRepository extends JpaRepository<SavedNumber, String> {
}
