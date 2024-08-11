package com.ticket.reporting.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ticket.reporting.TicketStatus;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Ticket {
    private Integer bookmaker;
    private String currency;
    private String game;
    private Double stake;
    private TicketStatus status;
    @JsonProperty("ticket_id")
    private String ticketId;
    @JsonProperty("total_return")
    private Double totalReturn;
    @JsonIgnore
    private boolean processed;

    public Integer getBookmaker() {
        return bookmaker;
    }

    public String getCurrency() {
        return currency;
    }

    public String getGame() {
        return game;
    }

    public Double getStake() {
        return stake;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public String getTicketId() {
        return ticketId;
    }

    public Double getTotalReturn() {
        return totalReturn;
    }

    public boolean isProcessed() {
        return processed;
    }

    public void setProcessed(boolean processed) {
        this.processed = processed;
    }
}
