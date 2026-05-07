package com.example.betting;

import com.example.betting.domain.BetStatus;
import com.example.betting.persistence.BetRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@EmbeddedKafka(partitions = 1, topics = {"event-outcomes", "event-outcomes.DLT"})
class EndToEndTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    BetRepository betRepository;

    @Test
    void fullFlow_eventOutcome_settlesBets() throws Exception {
        String body = """
                {
                  "eventId": "evt-1",
                  "eventName": "Real vs Barca",
                  "winnerId": "team-real"
                }
                """;

        mockMvc.perform(post("/api/event-outcomes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isAccepted());

        // Wait until all evt-1 PENDING bets are settled
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            var pending = betRepository.findPendingByEventId("evt-1");
            assertThat(pending).isEmpty();
        });

        // Verify correct WON/LOST outcomes based on winnerId
        var allBets = betRepository.findAll();
        var evt1Bets = allBets.stream().filter(b -> "evt-1".equals(b.getEventId())).toList();

        assertThat(evt1Bets).isNotEmpty();
        assertThat(evt1Bets).allMatch(b -> b.getStatus() != BetStatus.PENDING);

        // Bets placed on team-real must be WON with positive payout
        assertThat(evt1Bets)
                .filteredOn(b -> "team-real".equals(b.getWinnerId()))
                .allMatch(b -> b.getStatus() == BetStatus.WON)
                .allMatch(b -> b.getPayout() != null && b.getPayout().signum() > 0);

        // Bets placed on other teams must be LOST with zero payout
        assertThat(evt1Bets)
                .filteredOn(b -> !"team-real".equals(b.getWinnerId()))
                .allMatch(b -> b.getStatus() == BetStatus.LOST)
                .allMatch(b -> b.getPayout() != null && b.getPayout().signum() == 0);
    }
}
