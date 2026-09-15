package com.semsobra.backend.dto;

import java.math.BigDecimal;
import java.time.LocalTime;

public record ItemProducaoResponse(
        Long id,
        Long preparoId,
        String preparoNome,
        BigDecimal quantidadeProduzida,
        BigDecimal quantidadeSobra,
        boolean acabouAntesDoFim,
        LocalTime horarioAcabou
) {
}
