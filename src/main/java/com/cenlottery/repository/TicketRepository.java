package com.cenlottery.repository;

import com.cenlottery.domain.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, String> {
    List<Ticket> findByRoundNumber(long roundNumber);

    List<Ticket> findByPurchaseId(String purchaseId);
}
