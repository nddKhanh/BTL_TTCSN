package com.highlands.order.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record ProductRequest(@NotNull Long categoryId, @NotBlank @Size(max = 150) String name, @Size(max = 1000) String description,
                             @NotNull @DecimalMin("0.0") BigDecimal basePrice, @Size(max = 500) String imageUrl, @NotNull Boolean isActive) { }
