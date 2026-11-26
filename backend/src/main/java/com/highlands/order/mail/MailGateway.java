package com.highlands.order.mail;

public interface MailGateway {
    void send(String recipient, String subject, String body);
}
