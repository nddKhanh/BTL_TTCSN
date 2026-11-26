package com.highlands.order.mail;

import com.highlands.order.service.SuccessfulLoginEvent;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class LoginAlertListenerTest {
    @Test
    void masksIpAndDeduplicatesSameDevice() {
        FakeMailGateway mail = new FakeMailGateway();
        LoginAlertListener listener = new LoginAlertListener(mail);
        LocalDateTime now = LocalDateTime.now();

        listener.handle(new SuccessfulLoginEvent("user@example.com", now, "192.168.1.24", "Browser X"));
        listener.handle(new SuccessfulLoginEvent("user@example.com", now.plusMinutes(2), "192.168.1.99", "Browser X"));

        assertEquals(1, mail.outbox().size());
        assertFalse(mail.outbox().get(0).body().contains("192.168.1.24"));
    }
}
