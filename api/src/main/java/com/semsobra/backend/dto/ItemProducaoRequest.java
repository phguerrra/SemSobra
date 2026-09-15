package com.semsobra.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ItemProducaoRequest(
        @NotNull(message = "O preparo é obrigatório")
        @Positive(message = "O ID do preparo deve ser positivo")
        Long preparoId,

        @NotNull(message = "A quantidade produzida é obrigatória")
        @DecimalMin(value = "0.001", message = "A quantidade produzida deve ser maior que zero")
        @Digits(integer = 9, fraction = 3, message = "A quantidade produzida deve ter no máximo três casas decimais")
        BigDecimal quantidadeProduzida
) {
}
