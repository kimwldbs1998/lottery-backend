package com.cenlottery.service;

import com.cenlottery.domain.SavedNumber;
import com.cenlottery.repository.SavedNumberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Requirement section 9 "나만의 번호": exactly one saved favorite number per user. */
@Service
public class SavedNumberService {
    private final SavedNumberRepository savedNumberRepository;

    public SavedNumberService(SavedNumberRepository savedNumberRepository) {
        this.savedNumberRepository = savedNumberRepository;
    }

    public SavedNumber get(String userId) {
        return savedNumberRepository.findById(userId).orElse(null);
    }

    @Transactional
    public SavedNumber save(String userId, PurchaseService.Selection sel) {
        SavedNumber s = new SavedNumber();
        s.setUserId(userId);
        s.setGeneralBalls(sel.generalBalls);
        s.setPowerballs(sel.powerballs);
        s.setUpdatedAt(System.currentTimeMillis());
        return savedNumberRepository.save(s);
    }

    @Transactional
    public void delete(String userId) {
        savedNumberRepository.deleteById(userId);
    }
}
