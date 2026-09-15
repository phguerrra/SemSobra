package com.semsobra.backend.dto;

import java.math.BigDecimal;

public record RelatorioDesperdicioResponse(
        Long preparoId,
        String preparoNome,
        String unidadeMedida,
        BigDecimal quantidadeProduzida,
        BigDecimal quantidadeSobra,
        BigDecimal quantidadeConsumida,
        BigDecimal percentualDesperdicio,
        long vezesAcabouAntesDoFim
) {
}
