package com.cenlottery.web;

import com.cenlottery.domain.Round;
import com.cenlottery.exception.ApiException;
import com.cenlottery.repository.RoundRepository;
import com.cenlottery.repository.TicketRepository;
import com.cenlottery.service.EstimateService;
import com.cenlottery.service.RoundService;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rounds")
public class RoundController {
    private final RoundRepository roundRepository;
    private final TicketRepository ticketRepository;
    private final RoundService roundService;
    private final EstimateService estimateService;

    public RoundController(RoundRepository roundRepository, TicketRepository ticketRepository,
                            RoundService roundService, EstimateService estimateService) {
        this.roundRepository = roundRepository;
        this.ticketRepository = ticketRepository;
        this.roundService = roundService;
        this.estimateService = estimateService;
    }

    @GetMapping("/current")
    public Map<String, Object> current() {
        Round round = roundService.getCurrentRound();
        Map<String, Object> m = new LinkedHashMap<>(round.toJson());
        long remainingMs = Math.max(0, round.getEndTime() - System.currentTimeMillis());
        m.put("remainingSeconds", remainingMs / 1000);
        m.put("remainingMillis", remainingMs);
        m.put("saleOpen", Round.OPEN.equals(round.getStatus()) && remainingMs > 0);
        return m;
    }

    @GetMapping("/current/estimate")
    public Map<String, Object> currentEstimate() {
        Round round = roundService.getCurrentRound();
        return estimateService.estimate(round, ticketRepository.findByRoundNumber(round.getRoundNumber()));
    }

    @GetMapping("/{roundNumber}")
    public Map<String, Object> byNumber(@PathVariable String roundNumber) {
        long rn = parseRoundNumber(roundNumber);
        Round r = roundService.getRound(rn);
        if (r == null) throw ApiException.notFound("ROUND_NOT_FOUND", "해당 회차를 찾을 수 없습니다.");
        return r.toJson();
    }

    @GetMapping
    public Map<String, Object> list(@RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "20") int size) {
        var pageResult = roundRepository.findAllByOrderByRoundNumberDesc(PageRequest.of(page, size));
        List<Object> pageItems = new ArrayList<>();
        for (Round r : pageResult.getContent()) pageItems.add(r.toJson());
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("items", pageItems);
        m.put("total", pageResult.getTotalElements());
        m.put("page", page);
        m.put("size", size);
        return m;
    }

    private long parseRoundNumber(String s) {
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            throw ApiException.badRequest("INVALID_ROUND_NUMBER", "회차 번호가 올바르지 않습니다.");
        }
    }
}
