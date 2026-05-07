package com.example.betting.domain;

import java.math.BigDecimal;

public class BetSettlement {
    private Long betId;
    private String eventId;
    private BetStatus outcome;
    private BigDecimal payout;

    public BetSettlement() {}

    public BetSettlement(Long betId, String eventId, BetStatus outcome, BigDecimal payout) {
        this.betId = betId;
        this.eventId = eventId;
        this.outcome = outcome;
        this.payout = payout;
    }

    public Long getBetId() { return betId; }
    public void setBetId(Long betId) { this.betId = betId; }
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public BetStatus getOutcome() { return outcome; }
    public void setOutcome(BetStatus outcome) { this.outcome = outcome; }
    public BigDecimal getPayout() { return payout; }
    public void setPayout(BigDecimal payout) { this.payout = payout; }
}
