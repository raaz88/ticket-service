package com.ticket.reporting.consumer;

import com.ticket.reporting.TicketProcessingException;
import com.ticket.reporting.TicketStatus;
import com.ticket.reporting.model.CostMetrics;
import com.ticket.reporting.model.Ticket;
import com.ticket.reporting.model.TicketData;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class TicketConsumerTest {
    @Inject
    TicketConsumer ticketConsumer;

    private static final String message1 = "{\"bookmaker\": 1234," + "\"currency\": \"EUR\",\"game\": \"Call of Duty\"" +
            ",\"stake\": 123.45,\"status\": \"PLACED\",\"ticket_id\":\"34567\",\"total_return\": 234.56}";
    private static final String message2 = "{\"bookmaker\": 1234," + "\"currency\": \"PLN\",\"game\": \"NFS\"" +
            ",\"stake\": 234.56,\"status\": \"MONETISED\",\"ticket_id\":\"45678\",\"total_return\": 345.67}";
    private static final String message3 = "{\"bookmaker\": 1234," + "\"currency\": \"EUR\",\"game\": \"NFS\"" +
            ",\"stake\": 234.56,\"status\": \"MONETISED\",\"ticket_id\":\"56789\",\"total_return\": 345.67}";
    private static final String message4 = "{\"bookmaker\": 2345," + "\"currency\": \"INR\",\"game\": \"GTA\"" +
            ",\"stake\": 234.56,\"status\": \"CANCELLED\",\"ticket_id\":\"89012\"}";
    private static final String message5 = "{\"bookmaker\": 1234," + "\"currency\": \"EUR\",\"game\": \"Call of Duty\"" +
            ",\"stake\": 123.45,\"status\": \"MONETISED\",\"ticket_id\":\"34567\",\"total_return\": 234.56}";
    private static final String invalidMessage1 = "{\"bookmaker\": 12345," + "\"currency\": \"EUR\"," +
            "\"game\": \"Call of Duty\",\"stake\": 123.45,\"ticket_id\":\"345678\",\"total_return\": 234.56}";
    private static final String invalidMessage2 = "{\"bookmaker\": 09876," + "\"currency\": EUR," +
            "\"game\": \"Call of Duty\",\"stake\": 123.45,\"ticket_id\":098765,\"total_return\": 234.56}";

    @Test
    void testInvalidIncomingMessage() {
        // when
        ticketConsumer.consume(invalidMessage1);
        List<TicketData> ticketsData = ticketConsumer.getTicketsByBookmaker(12345);
        // then
        Ticket ticket = ticketsData.stream()
                .filter(ticketData -> "345678".equals(ticketData.ticketId()))
                .map(TicketData::ticket).findFirst().orElse(null);
        assertNull(ticket);
    }

    @Test
    void testIncorrectIncomingMessage_ThrowsException() {
        // when
        TicketProcessingException thrown = assertThrows(TicketProcessingException.class,
                () -> ticketConsumer.consume(invalidMessage2));
        assertTrue(thrown.getMessage().contains("Invalid numeric value"));
    }

    @Test
    void testValidIncomingMessage() {
        // when
        ticketConsumer.consume(message4);
        List<TicketData> ticketsData = ticketConsumer.getTicketsByBookmaker(2345);
        // then
        Ticket ticket = ticketsData.stream()
                .filter(ticketData -> "89012".equals(ticketData.ticketId()))
                .map(TicketData::ticket).findFirst().orElse(null);
        assertNotNull(ticket);
        assertEquals(1, ticketsData.size());
        assertEquals(2345, ticket.getBookmaker());
        assertEquals("GTA", ticket.getGame());
    }

    @Test
    void testConsumeAndGetAllTickets() {
        // given
        String ticketId = "34567";
        // when
        ticketConsumer.consume(message1);
        List<TicketData> ticketsData = ticketConsumer.getTickets();
        // then
        Ticket ticket = ticketsData.stream()
                .filter(ticketData -> ticketId.equals(ticketData.ticketId()))
                .map(TicketData::ticket).findFirst().orElse(null);
        assertNotNull(ticket);
        assertEquals(4, ticketsData.size());
        assertEquals(1234, ticket.getBookmaker());
        assertEquals("Call of Duty", ticket.getGame());
    }

    @Test
    void testRetrieveTicketsByBookmakerId() {
        // given
        int bookmakerId = 1234;
        // when
        ticketConsumer.consume(message1);
        ticketConsumer.consume(message2);
        List<TicketData> ticketsData = ticketConsumer.getTicketsByBookmaker(bookmakerId);
        // then
        List<String> ticketIds = ticketsData.stream().map(TicketData::ticketId).toList();
        List<Integer> bookmakerIds =
                ticketsData.stream().map(ticketData -> ticketData.ticket().getBookmaker()).toList();
        assertEquals(1234, bookmakerIds.get(0));
        assertEquals(3, ticketsData.size());
        assertEquals(List.of("34567", "45678", "56789"), ticketIds);
    }

    @Test
    void testRetrieveTicketsByBookmakerIdGroupedByGame() {
        // given
        int bookmakerId = 1234;
        // when
        ticketConsumer.consume(message1);
        ticketConsumer.consume(message2);
        ticketConsumer.consume(message3);
        ticketConsumer.consume(message4);
        Map<String, CostMetrics> costMetricsByGame = ticketConsumer.getTicketsByBookmakerIdGroupedByGame(bookmakerId);
        // then
        List<String> games = costMetricsByGame.keySet().stream().toList();
        assertEquals(2, costMetricsByGame.size());
        assertEquals(List.of("Call of Duty", "NFS"), games);
        assertEquals(1, costMetricsByGame.get("Call of Duty").numberOfTickets());
        assertEquals(2, costMetricsByGame.get("NFS").numberOfTickets());
    }

    @Test
    void testLatestReceivedEventHasCurrentStatus() {
        // given
        String ticketId = "34567";
        // when
        ticketConsumer.consume(message1);
        ticketConsumer.consume(message5);
        List<TicketData> ticketsData = ticketConsumer.getTickets();
        // then
        Ticket ticket = ticketsData.stream()
                .filter(ticketData -> ticketId.equals(ticketData.ticketId()))
                .map(TicketData::ticket).findFirst().orElse(null);
        assertNotNull(ticket);
        assertEquals(4, ticketsData.size());
        assertEquals(1234, ticket.getBookmaker());
        assertEquals("Call of Duty", ticket.getGame());
        assertEquals(TicketStatus.MONETISED, ticket.getStatus());
    }
}