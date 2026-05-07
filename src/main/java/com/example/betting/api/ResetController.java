package com.example.betting.api;

import org.jooq.DSLContext;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

import static com.example.betting.jooq.Tables.BETS;

@RestController
@RequestMapping("/api")
public class ResetController {

    private final DSLContext dsl;

    public ResetController(DSLContext dsl) {
        this.dsl = dsl;
    }

    @PostMapping("/reset")
    @Transactional
    public ResponseEntity<Void> reset() {
        dsl.deleteFrom(BETS).execute();
        dsl.insertInto(BETS, BETS.USER_ID, BETS.EVENT_ID, BETS.MARKET_ID, BETS.WINNER_ID, BETS.AMOUNT, BETS.STATUS)
                .values("user-1", "evt-1", "market-1h", "team-real",    new BigDecimal("100.00"), "PENDING")
                .values("user-2", "evt-1", "market-1h", "team-barca",   new BigDecimal("200.00"), "PENDING")
                .values("user-3", "evt-1", "market-1h", "team-real",    new BigDecimal("50.00"),  "PENDING")
                .values("user-4", "evt-2", "market-2h", "team-arsenal", new BigDecimal("150.00"), "PENDING")
                .values("user-5", "evt-2", "market-2h", "team-chelsea", new BigDecimal("75.00"),  "PENDING")
                .execute();
        return ResponseEntity.noContent().build();
    }
}
