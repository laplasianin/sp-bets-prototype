package com.example.betting.persistence;

import com.example.betting.domain.Bet;
import com.example.betting.domain.BetStatus;
import com.example.betting.jooq.tables.records.BetsRecord;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.example.betting.jooq.Tables.BETS;

@Repository
public class JooqBetRepository implements BetRepository {

    private final DSLContext dsl;

    public JooqBetRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    public List<Bet> findPendingByEventId(String eventId) {
        return dsl.selectFrom(BETS)
                .where(BETS.EVENT_ID.eq(eventId))
                .and(BETS.STATUS.eq(BetStatus.PENDING.name()))
                .fetch()
                .map(this::toDomain);
    }

    @Override
    public Optional<Bet> findById(Long id) {
        return dsl.selectFrom(BETS)
                .where(BETS.ID.eq(id))
                .fetchOptional()
                .map(this::toDomain);
    }

    @Override
    public List<Bet> findAll() {
        return dsl.selectFrom(BETS)
                .fetch()
                .map(this::toDomain);
    }

    @Override
    @Transactional
    public void settle(Long betId, BetStatus status, BigDecimal payout) {
        // H2 rejects table-qualified columns in UPDATE SET with CASE_INSENSITIVE_IDENTIFIERS=TRUE
        dsl.update(BETS)
                .set(DSL.field(DSL.name("status"), String.class), status.name())
                .set(DSL.field(DSL.name("payout"), BigDecimal.class), payout)
                .set(DSL.field(DSL.name("settled_at"), LocalDateTime.class), LocalDateTime.now())
                .where(BETS.ID.eq(betId))
                .and(BETS.STATUS.eq(BetStatus.PENDING.name()))
                .execute();
    }

    private Bet toDomain(BetsRecord r) {
        Bet bet = new Bet();
        bet.setId(r.getId());
        bet.setUserId(r.getUserId());
        bet.setEventId(r.getEventId());
        bet.setMarketId(r.getMarketId());
        bet.setWinnerId(r.getWinnerId());
        bet.setAmount(r.getAmount());
        bet.setStatus(BetStatus.valueOf(r.getStatus()));
        bet.setPayout(r.getPayout());
        bet.setSettledAt(r.getSettledAt());
        bet.setCreatedAt(r.getCreatedAt());
        return bet;
    }
}
