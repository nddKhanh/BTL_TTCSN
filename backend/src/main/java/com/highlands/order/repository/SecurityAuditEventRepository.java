package com.highlands.order.repository;

import com.highlands.order.model.SecurityAuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SecurityAuditEventRepository extends JpaRepository<SecurityAuditEvent, Long>,
        JpaSpecificationExecutor<SecurityAuditEvent> { }
