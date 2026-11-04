package com.highlands.order.mail;

import com.highlands.order.service.SuccessfulLoginEvent;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginAlertListener {
    private static final Duration DEDUPE_WINDOW = Duration.ofMinutes(10);
    private final MailGateway mail;
    private final ConcurrentHashMap<String, LocalDateTime> sent = new ConcurrentHashMap<>();

    public LoginAlertListener(MailGateway mail) {
        this.mail = mail;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(SuccessfulLoginEvent event) {
        String device = compact(event.userAgent());
        String ip = maskIp(event.ipAddress());
        String key = event.email() + '|' + ip + '|' + device;
        LocalDateTime previous = sent.putIfAbsent(key, event.occurredAt());
        if (previous != null && Duration.between(previous, event.occurredAt()).compareTo(DEDUPE_WINDOW) < 0) return;
        sent.put(key, event.occurredAt());
        mail.send(event.email(), "Đăng nhập mới vào Mộc Nhiên Coffee",
                "Thời gian: " + event.occurredAt() + "\nThiết bị: " + device + "\nIP: " + ip
                        + "\nNếu không phải bạn, hãy đổi mật khẩu và đăng xuất mọi thiết bị.");
    }

    static String maskIp(String value) {
        if (value == null || value.isBlank()) return "không xác định";
        String[] parts = value.split("\\.");
        return parts.length == 4 ? parts[0] + "." + parts[1] + ".x.x" : "đã ẩn";
    }

    private static String compact(String value) {
        if (value == null || value.isBlank()) return "không xác định";
        String sanitized = value.replaceAll("[\\r\\n]", " ");
        return sanitized.substring(0, Math.min(sanitized.length(), 80));
    }
}
