package com.highlands.order.dto;

import jakarta.validation.constraints.*;

public record CategoryRequest(@NotBlank @Size(max = 50) String code, @NotBlank @Size(max = 100) String name, @Size(max = 500) String imageUrl) { }
