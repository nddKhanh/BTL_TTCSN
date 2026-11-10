package com.highlands.order.domain.approval;

import java.util.Set;

public record ApprovalActor(
        String actorId,
        Long userId,
        boolean authenticated,
        Set<String> authorities
) {
    public ApprovalActor {
        authorities = authorities == null ? Set.of() : Set.copyOf(authorities);
    }

    public static ApprovalActor anonymous() {
        return new ApprovalActor("anonymous", null, false, Set.of());
    }

    public static ApprovalActor guestOrder(String orderCode) {
        return new ApprovalActor("guest-order:" + orderCode, null, false, Set.of());
    }

    public boolean hasAuthority(String authority) {
        return authorities.contains(authority);
    }
}
