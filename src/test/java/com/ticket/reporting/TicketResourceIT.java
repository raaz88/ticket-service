package com.ticket.reporting;

import com.ticket.reporting.consumer.TicketConsumer;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasKey;

@QuarkusTest
class TicketResourceIT {
    @Inject
    TicketConsumer ticketConsumer;

    private static final String message1 = "{\"bookmaker\": 1234," + "\"currency\": \"EUR\",\"game\": \"Call of Duty\"" +
            ",\"stake\": 123.45,\"status\": \"PLACED\",\"ticket_id\":\"34567\",\"total_return\": 234.56}";
    private static final String message2 = "{\"bookmaker\": 1234," + "\"currency\": \"PLN\",\"game\": \"NFS\"" +
            ",\"stake\": 234.56,\"status\": \"MONETISED\",\"ticket_id\":\"45678\",\"total_return\": 345.67}";
    private static final String message3 = "{\"bookmaker\": 1234," + "\"currency\": \"EUR\",\"game\": \"NFS\"" +
            ",\"stake\": 234.56,\"status\": \"MONETISED\",\"ticket_id\":\"56789\",\"total_return\": 345.67}";
    private static final String message4 = "{\"bookmaker\": 2345," + "\"currency\": \"INR\",\"game\": \"GTA\"" +
            ",\"stake\": 234.56,\"status\": \"CANCELLED\",\"ticket_id\":\"89012\",\"total_return\": 345.67}";

    @BeforeEach
    public void setup() {
        // Simulate receiving tickets
        ticketConsumer.consume(message1);
        ticketConsumer.consume(message2);
        ticketConsumer.consume(message3);
        ticketConsumer.consume(message4);
    }

    @Test
    void testKafkaConsumer() {
        // Verify the ticket is processed and stored correctly
        given()
                .when().get("/tickets")
                .then()
                .statusCode(200)
                .body("$.size()", is(4))
                .body("[0].ticketId", is("34567"));
    }

    @Test
    void testGetTicketsEndpoint() {
        given()
                .when().get("/tickets")
                .then()
                .statusCode(200)
                .body("$.size()", is(4));
    }

    @Test
    void testGetTicketsByBookmakerIdEndpoint() {
        given()
                .when().get("/tickets/bookmaker/1234")
                .then()
                .statusCode(200)
                .body("numberOfTickets", is(3))
                .body("turnover", is(592.57F))
                .body("profitLoss", is(333.33F))
                .body("margin", is(56.25F));
    }

    @Test
    void testGetTicketsGroupedByGameEndpoint() {
        given()
                .when().get("/tickets/bookmaker/1234/product")
                .then()
                .statusCode(200)
                .body("$", hasKey("Call of Duty"))
                .body("$", hasKey("NFS"))
                .body("'Call of Duty'.numberOfTickets", is(1))
                .body("'Call of Duty'.turnover", is(123.45F))
                .body("'Call of Duty'.profitLoss", is(111.11F))
                .body("'Call of Duty'.margin", is(90.0F))
                .body("NFS.numberOfTickets", is(2))
                .body("NFS.turnover", is(469.12F))
                .body("NFS.profitLoss", is(222.22F))
                .body("NFS.margin", is(47.37F));
    }
}
