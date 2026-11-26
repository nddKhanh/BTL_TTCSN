package com.highlands.order.security;

import com.highlands.order.model.AppUser;
import com.highlands.order.repository.UserRepository;
import com.highlands.order.repository.RolePermissionRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DatabaseUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;
    private final RolePermissionRepository rolePermissions;
    public DatabaseUserDetailsService(UserRepository userRepository, RolePermissionRepository rolePermissions) {
        this.userRepository = userRepository; this.rolePermissions = rolePermissions;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        AppUser user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng"));
        var authorities = new java.util.ArrayList<SimpleGrantedAuthority>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
        rolePermissions.findAllByRole(user.getRole()).stream()
                .map(item -> new SimpleGrantedAuthority(item.getPermission().name()))
                .forEach(authorities::add);
        return User.withUsername(user.getEmail()).password(user.getPasswordHash()).authorities(authorities).build();
    }
}
