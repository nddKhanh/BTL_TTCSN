package com.highlands.order.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record ToppingRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull @DecimalMin("0.0") BigDecimal price
) { }
