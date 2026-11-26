package com.highlands.order.repository;

import com.highlands.order.model.Role;
import com.highlands.order.model.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {
    List<RolePermission> findAllByRole(Role role);
}
