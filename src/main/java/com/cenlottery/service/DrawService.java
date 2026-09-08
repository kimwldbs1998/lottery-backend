package com.cenlottery.service;

import com.cenlottery.util.Constants;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Performs the fair random draw: 5 distinct general balls out of 1..7, and 1 powerball out of 0..9. */
@Service
public class DrawService {
    private final SecureRandom random = new SecureRandom();

    public List<Integer> drawGeneralBalls() {
        List<Integer> pool = new ArrayList<>();
        for (int i = Constants.GENERAL_BALL_MIN; i <= Constants.GENERAL_BALL_MAX; i++) pool.add(i);
        Collections.shuffle(pool, random);
        List<Integer> drawn = new ArrayList<>(pool.subList(0, Constants.GENERAL_BALL_PICK));
        Collections.sort(drawn);
        return drawn;
    }

    public int drawPowerball() {
        return Constants.POWERBALL_MIN + random.nextInt(Constants.POWERBALL_MAX - Constants.POWERBALL_MIN + 1);
    }

    /**
     * Tier for a ticket given how many of its general balls matched the draw and whether its
     * powerball matched. Returns 1-5, or null for no win. Because both the pick and the draw are
     * 5-of-7, matches is always 3, 4, or 5 (pigeonhole: |A cap B| >= 5+5-7 = 3).
     */
    public Integer tierFor(int generalMatches, boolean powerballMatch) {
        if (generalMatches == 5 && powerballMatch) return 1;
        if (generalMatches == 5) return 2;
        if (generalMatches == 4 && powerballMatch) return 3;
        if (generalMatches == 4) return 4;
        if (generalMatches == 3 && powerballMatch) return 5;
        return null; // 3 matches, powerball miss -> no win
    }

    public int countMatches(List<Integer> selected, List<Integer> drawn) {
        int count = 0;
        for (int n : selected) if (drawn.contains(n)) count++;
        return count;
    }
}
