package com.semsobra.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;

public record FechamentoProducaoRequest(
        @NotNull @PositiveOrZero Integer clientesAtendidos,
        @NotNull Boolean restauranteAberto,
        @NotEmpty List<@Valid FechamentoItemRequest> itens
) {
}
