package com.highlands.order.dto;

import jakarta.validation.constraints.*;

public record UpdateProfileRequest(@NotBlank @Size(max = 100) String fullName, @Size(max = 20) String phone) { }
