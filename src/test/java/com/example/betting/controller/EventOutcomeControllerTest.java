package com.example.betting.controller;

import com.example.betting.api.EventOutcomeController;
import com.example.betting.mapper.EventOutcomeMapper;
import com.example.betting.messaging.kafka.EventOutcomeProducer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EventOutcomeController.class)
class EventOutcomeControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    EventOutcomeProducer producer;

    @MockBean
    EventOutcomeMapper outcomeMapper;

    @Test
    void publishOutcome_returns202() throws Exception {
        doNothing().when(producer).publish(any());
        when(outcomeMapper.toDomain(any())).thenReturn(
                new com.example.betting.domain.EventOutcome("evt-1", "Real vs Barca", "team-real"));

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
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.eventId").value("evt-1"))
                .andExpect(jsonPath("$.acceptedAt").exists())
                .andExpect(header().exists("Location"));
    }

    @Test
    void publishOutcome_withMissingField_returns400() throws Exception {
        String body = """
                {
                  "eventName": "Real vs Barca"
                }
                """;
        mockMvc.perform(post("/api/event-outcomes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
