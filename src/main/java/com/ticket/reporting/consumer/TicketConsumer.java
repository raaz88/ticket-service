package com.ticket.reporting.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticket.reporting.TicketProcessingException;
import com.ticket.reporting.model.CostMetrics;
import com.ticket.reporting.model.Ticket;
import com.ticket.reporting.model.TicketData;
import io.quarkus.logging.Log;
import io.quarkus.runtime.Startup;
import io.restassured.module.jsv.JsonSchemaValidator;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Incoming;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Kafka consumer that listens to ticket stream and stores them in memory.
 */
@ApplicationScoped
@Startup
public class TicketConsumer {
    private static final int PLACES_TO_ROUND = 2;
    private static final String JSON_SCHEMA_NAME = "ticket.v1.json";
    private final Map<String, Ticket> ticketStore = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Consumes messages from the Kafka topic and updates the ticket store.
     *
     * @param message - message received from the Kafka topic
     */
    @Incoming("tickets")
    public void consume(String message) {
        try {
            Log.info("ticketJson: " + message);
            if (incomingMessageValid(message)) {
                Ticket ticket = objectMapper.readValue(message, Ticket.class);
                Log.info("ticketJson: " + message);
                if (ticket != null) {
                    ticket.setProcessed(true);
                    ticketStore.put(ticket.getTicketId(), ticket);
                    Log.info("ticketsData: " + ticketStore);
                }
            }
        } catch (RuntimeException | IOException e) {
            Log.info("Exception occurred while processing: " + e.getMessage());
            throw new TicketProcessingException(e.getMessage());
        }
    }

    /**
     * Retrieves all tickets from the ticketStore
     *
     * @return List<TicketData> - List of all tickets in the store
     */
    public List<TicketData> getTickets() {
        return ticketStore.values().stream()
                .map(TicketConsumer::createTicketData).toList();
    }

    /**
     * Retrieves the list of tickets for a specific bookmakerId
     *
     * @param bookmakerId - Bookmaker Id based on which ticket data has to be retrieved
     * @return List<TicketData> - List of all tickets filtered by bookmakerId
     */
    public List<TicketData> getTicketsByBookmaker(int bookmakerId) {
        return ticketStore.values().stream()
                .filter(ticket -> bookmakerId == ticket.getBookmaker())
                .map(TicketConsumer::createTicketData)
                .toList();
    }

    /**
     * Retrieves the CostMetrics for a specific bookmakerId grouped by Product
     *
     * @param bookmakerId - Bookmaker Id based on which ticket data has to be retrieved
     * @return Map<String, CostMetrics> - List of all tickets filtered by bookmakerId and grouped by Product
     */
    public Map<String, CostMetrics> getTicketsByBookmakerIdGroupedByGame(int bookmakerId) {
        return getTicketsByBookmaker(bookmakerId).stream()
                .collect(Collectors.groupingBy(ticketData -> ticketData.ticket().getGame(),
                        Collectors.collectingAndThen(Collectors.toList(), this::calculateCostMetrics)));
    }

    /**
     * Calculates the Cost Metrics like turnover, profit Or Loss and Margin for the tickets
     *
     * @param ticketsData - Ticket Data for which the CostMetrics needs to be calculated
     * @return CostMetrics
     */
    public CostMetrics calculateCostMetrics(List<TicketData> ticketsData) {
        List<Ticket> tickets = ticketsData.stream().flatMap(ticketData -> Stream.of(ticketData.ticket())).toList();
        double turnover = roundValue(tickets.stream().mapToDouble(Ticket::getStake).sum());
        double profitOrLoss =
                roundValue(tickets.stream().mapToDouble(ticket -> ticket.getTotalReturn() - ticket.getStake()).sum());
        double margin = (turnover > 0 && profitOrLoss != 0) ? roundValue(profitOrLoss / turnover * 100) : 0;
        return new CostMetrics(ticketsData.size(), turnover, profitOrLoss, margin);
    }

    /**
     * Method to create a TicketData record based on ticketId and ticket
     *
     * @param ticket - Ticket for which TicketData has to be created
     * @return TicketData
     */
    private static TicketData createTicketData(Ticket ticket) {
        return new TicketData(ticket.getTicketId(), ticket);
    }

    /**
     * Method to validate incoming message against JSON Schema
     *
     * @param message - Incoming message
     * @return boolean
     */
    private boolean incomingMessageValid(String message) throws IOException {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(JSON_SCHEMA_NAME)) {
            return inputStream != null && JsonSchemaValidator.matchesJsonSchema(inputStream).matches(message);
        }
    }

    /**
     * Method to round values to 2 decimal places
     *
     * @param costParam - Value to be rounded to 2 decimal places
     * @return double
     */
    private static double roundValue(double costParam) {
        BigDecimal value = new BigDecimal(Double.toString(costParam));
        value = value.setScale(PLACES_TO_ROUND, RoundingMode.HALF_UP);
        return value.doubleValue();
    }
}
