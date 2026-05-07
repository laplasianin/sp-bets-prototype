package com.example.betting.domain;

public class EventOutcome {
    private String eventId;
    private String eventName;
    private String winnerId;

    public EventOutcome() {}

    public EventOutcome(String eventId, String eventName, String winnerId) {
        this.eventId = eventId;
        this.eventName = eventName;
        this.winnerId = winnerId;
    }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }
    public String getWinnerId() { return winnerId; }
    public void setWinnerId(String winnerId) { this.winnerId = winnerId; }
}
