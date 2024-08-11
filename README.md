# UML Diagram

<img src="D:\Raaz\Dev stuff\Interview\Sportradar\TicketReportingService.png"/>

# OpenAPI Specification for the REST APIs

    http://localhost:8080/swagger-ui

# TicketConsumer

    Kafka consumer that listens to ticket stream and stores them in memory.

# Ticket

    Incoming message mapped to Ticket and validated against the provided schema at src/main/resources/ticket.v1.json

# TicketResource

    Exposes REST APIs to retrieve tickets by bookmakerId and by bookmakerId grouped by game

# CostMetrics

    Contains fields numberOfTickets, turnover, profitLoss & margin to be returned int the response. 
    Formulae to calculate turnover, profitLoss & margin based on assumptions, can be modified as required. 

# Dockerfile

    To package the service as a container image and run the container