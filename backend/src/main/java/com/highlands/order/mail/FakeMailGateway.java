package com.highlands.order.mail;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class FakeMailGateway implements MailGateway {
    private final List<MailMessage> outbox = new CopyOnWriteArrayList<>();

    @Override
    public void send(String recipient, String subject, String body) {
        outbox.add(new MailMessage(recipient, subject, body));
    }

    public List<MailMessage> outbox() {
        return List.copyOf(outbox);
    }

    public record MailMessage(String recipient, String subject, String body) { }
}
