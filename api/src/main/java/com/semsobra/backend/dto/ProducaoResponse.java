package com.semsobra.backend.dto;

import com.semsobra.backend.entity.Turno;

import java.time.LocalDate;
import java.util.List;

public record ProducaoResponse(
        Long id,
        LocalDate data,
        Turno turno,
        int clientesAtendidos,
        boolean restauranteAberto,
        boolean fechado,
        List<ItemProducaoResponse> itens
) {
}
