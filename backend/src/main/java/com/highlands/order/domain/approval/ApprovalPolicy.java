package com.highlands.order.domain.approval;

import java.util.EnumSet;
import java.util.Set;

public final class ApprovalPolicy {

    private ApprovalPolicy() {
    }

    public static Set<ApprovalAction> availableActions(
            ApprovalStatus status,
            String ownerActorId,
            ApprovalActor actor
    ) {
        if (status != ApprovalStatus.PENDING) {
            return Set.of();
        }

        EnumSet<ApprovalAction> actions = EnumSet.noneOf(ApprovalAction.class);
        boolean owner = actor.authenticated() && actor.actorId().equals(ownerActorId);
        if (owner) {
            actions.add(ApprovalAction.WITHDRAW);
        }
        if (actor.hasAuthority("ORDER_APPROVE") && !actor.actorId().equals(ownerActorId)) {
            actions.add(ApprovalAction.APPROVE);
            actions.add(ApprovalAction.REJECT);
        }
        return Set.copyOf(actions);
    }

    public static boolean canRead(String ownerActorId, ApprovalActor actor) {
        return actor.authenticated()
                && (actor.actorId().equals(ownerActorId) || actor.hasAuthority("ORDER_READ"));
    }
}
