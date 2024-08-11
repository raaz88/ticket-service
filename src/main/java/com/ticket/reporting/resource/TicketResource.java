package com.ticket.reporting.resource;

import com.ticket.reporting.model.TicketData;
import com.ticket.reporting.model.CostMetrics;
import com.ticket.reporting.consumer.TicketConsumer;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;

import java.util.List;
import java.util.Map;

@Path("/tickets")
public class TicketResource {
    private final TicketConsumer ticketConsumer;

    public TicketResource(TicketConsumer ticketConsumer) {
        this.ticketConsumer = ticketConsumer;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get all tickets", description = "Returns a list of all tickets")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "All tickets",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = TicketData.class)))
    })
    public List<TicketData> getAllTickets() {
        return ticketConsumer.getTickets();
    }

    @GET
    @Path("/bookmaker/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get a list of all tickets by Bookmaker ID",
            description = "Returns a list of all tickets by Bookmaker ID")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Cost Metrics for Bookmaker ID",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CostMetrics.class)))
    })
    public CostMetrics getTicketsByBookmaker(@PathParam("id") int bookmakerId) {
        List<TicketData> ticketsData = ticketConsumer.getTicketsByBookmaker(bookmakerId);
        return ticketConsumer.calculateCostMetrics(ticketsData);
    }

    @GET
    @Path("/bookmaker/{id}/product")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get a list of all tickets by Bookmaker ID grouped by Game",
            description = "Returns a list of all tickets by Bookmaker ID grouped by Game")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Cost Metrics for Bookmaker ID by Game",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Map.class)))
    })
    public Map<String, CostMetrics> getTicketsByBookmakerIdGroupedByGame(@PathParam("id") int bookmakerId) {
        return ticketConsumer.getTicketsByBookmakerIdGroupedByGame(bookmakerId);
    }
}
