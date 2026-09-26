package com.highlands.order.dto;

import com.highlands.order.model.AppUser;
import com.highlands.order.model.Role;

public record UserResponse(Long id, String fullName, String email, String phone, Role role) {
    public static UserResponse from(AppUser user) {
        return new UserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getPhone(), user.getRole());
    }
}
