package com.highlands.order.mail;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FakeMailGatewayTest {
    @Test
    void capturesMailWithoutNetworkAccess() {
        FakeMailGateway gateway = new FakeMailGateway();

        gateway.send("customer@example.com", "Verify", "token");

        assertEquals(1, gateway.outbox().size());
        assertEquals("customer@example.com", gateway.outbox().get(0).recipient());
    }
}
