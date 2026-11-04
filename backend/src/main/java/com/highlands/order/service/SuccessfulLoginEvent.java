package com.highlands.order.service;

import java.time.LocalDateTime;

public record SuccessfulLoginEvent(String email, LocalDateTime occurredAt,
                                   String ipAddress, String userAgent) { }
