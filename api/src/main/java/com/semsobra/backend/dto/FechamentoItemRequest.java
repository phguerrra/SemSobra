package com.semsobra.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalTime;

public record FechamentoItemRequest(
        @NotNull @Positive Long itemId,
        @NotNull @DecimalMin("0.000") @Digits(integer = 9, fraction = 3) BigDecimal quantidadeSobra,
        boolean acabouAntesDoFim,
        LocalTime horarioAcabou
) {
}
