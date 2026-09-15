package com.semsobra.backend.dto;

import com.semsobra.backend.entity.Turno;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record ProducaoRequest(
        @NotNull(message = "A data é obrigatória")
        LocalDate data,

        @NotNull(message = "O turno é obrigatório")
        Turno turno,

        @NotEmpty(message = "A produção deve possuir ao menos um item")
        List<@Valid ItemProducaoRequest> itens
) {
}
