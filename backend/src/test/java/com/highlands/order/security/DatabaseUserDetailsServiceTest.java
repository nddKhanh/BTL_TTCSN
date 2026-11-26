package com.highlands.order.security;

import com.highlands.order.model.*;
import com.highlands.order.repository.RolePermissionRepository;
import com.highlands.order.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class DatabaseUserDetailsServiceTest {
    @Test
    void loadsRoleAndGrantedPermissions() {
        UserRepository users = mock(UserRepository.class);
        RolePermissionRepository grants = mock(RolePermissionRepository.class);
        AppUser admin = AppUser.builder().email("admin@example.com").passwordHash("hash").role(Role.ADMIN).build();
        when(users.findByEmailIgnoreCase(admin.getEmail())).thenReturn(Optional.of(admin));
        when(grants.findAllByRole(Role.ADMIN)).thenReturn(List.of(
                RolePermission.builder().role(Role.ADMIN).permission(Permission.REPORT_READ).build()));

        var details = new DatabaseUserDetailsService(users, grants).loadUserByUsername(admin.getEmail());

        assertTrue(details.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        assertTrue(details.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("REPORT_READ")));
    }
}
