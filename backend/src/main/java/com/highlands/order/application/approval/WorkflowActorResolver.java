package com.highlands.order.application.approval;

import com.highlands.order.domain.approval.ApprovalActor;
import com.highlands.order.exception.ResourceNotFoundException;
import com.highlands.order.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class WorkflowActorResolver {

    private final UserRepository users;

    public WorkflowActorResolver(UserRepository users) {
        this.users = users;
    }

    public ApprovalActor resolve(UserDetails principal) {
        if (principal == null) {
            return ApprovalActor.anonymous();
        }
        var user = users.findByEmailIgnoreCase(principal.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản hiện tại"));
        var authorities = principal.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toSet());
        return new ApprovalActor("user:" + user.getId(), user.getId(), true, authorities);
    }
}
