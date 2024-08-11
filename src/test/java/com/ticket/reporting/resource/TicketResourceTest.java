package com.ticket.reporting.resource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticket.reporting.model.CostMetrics;
import com.ticket.reporting.model.Ticket;
import com.ticket.reporting.model.TicketData;
import com.ticket.reporting.consumer.TicketConsumer;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@QuarkusTest
class TicketResourceTest {
    @InjectMocks
    TicketResource ticketResource;
    @Mock
    TicketConsumer ticketConsumer;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetAllTickets() throws JsonProcessingException {
        // given
        List<TicketData> ticketsData = List.of(
                new TicketData("34567", tickets().get(0)),
                new TicketData("45678", tickets().get(1))
        );
        when(ticketConsumer.getTickets()).thenReturn(ticketsData);
        // when
        List<TicketData> tickets = ticketResource.getAllTickets();
        // then
        assertEquals(2, tickets.size());
        assertEquals(1234, tickets.get(0).ticket().getBookmaker());
        assertEquals("34567", tickets.get(0).ticketId());
    }

    @Test
    void testGetTicketsByBookMaker() throws JsonProcessingException {
        // given
        List<TicketData> ticketsDataByBookmaker = List.of(
                new TicketData("34567", tickets().get(0)),
                new TicketData("45678", tickets().get(1))
        );
        when(ticketConsumer.getTicketsByBookmaker(1234)).thenReturn(ticketsDataByBookmaker);
        when(ticketConsumer.calculateCostMetrics(ticketsDataByBookmaker))
                .thenReturn(new CostMetrics(3, 592.57, 333.33, 56.25));
        // when
        CostMetrics costMetrics = ticketResource.getTicketsByBookmaker(1234);
        // then
        assertEquals(3, costMetrics.numberOfTickets());
        assertEquals(592.57, costMetrics.turnover());
        assertEquals(333.33, costMetrics.profitLoss());
        assertEquals(56.25, costMetrics.margin());
    }

    @Test
    void testGetTicketsByBookMakerGroupedByGame() {
        // given
        when(ticketConsumer.getTicketsByBookmakerIdGroupedByGame(1234)).thenReturn(costMetricsByGame());
        // when
        Map<String, CostMetrics> costMetricsByGame = ticketResource.getTicketsByBookmakerIdGroupedByGame(1234);
        // then
        assertEquals(2, costMetricsByGame.size());
        assert (costMetricsByGame.keySet().stream().toList().contains("NFS"));
        assertEquals(1, costMetricsByGame().get("Call of Duty").numberOfTickets());
        assertEquals(2, costMetricsByGame().get("NFS").numberOfTickets());
    }

    private Map<String, CostMetrics> costMetricsByGame() {
        return Map.of("Call of Duty", new CostMetrics(1, 123.45, 111.11, 90.04),
                "NFS", new CostMetrics(2, 469.12, 222.22, 47.37));
    }

    private static List<Ticket> tickets() throws JsonProcessingException {
        return List.of(objectMapper.readValue(
                        "{\"bookmaker\": 1234," + "\"currency\": \"EUR\",\"game\": \"Call of Duty\"" +
                                ",\"stake\": 123.45,\"status\": \"PLACED\",\"ticket_id\":\"34567\",\"total_return\": 234.56}",
                        Ticket.class),
                objectMapper.readValue(
                        "{\"bookmaker\": 1234," + "\"currency\": \"PLN\",\"game\": \"NFS\"" +
                                ",\"stake\": 234.56,\"status\": \"MONETISED\",\"ticket_id\":\"45678\"," +
                                "\"total_return\": 345.67}",
                        Ticket.class),
                objectMapper.readValue(
                        "{\"bookmaker\": 2345," + "\"currency\": \"INR\",\"game\": \"NFS\"" +
                                ",\"stake\": 234.56,\"status\": \"CANCELLED\",\"ticket_id\":\"89012\"," +
                                "\"total_return\": 345.67}",
                        Ticket.class),
                objectMapper.readValue(
                        "{\"bookmaker\": 1234," + "\"currency\": \"INR\",\"game\": \"NFS\"" +
                                ",\"stake\": 234.56,\"status\": \"PLACED\",\"ticket_id\":\"78901\"," +
                                "\"total_return\": 345.67}",
                        Ticket.class));
    }
}