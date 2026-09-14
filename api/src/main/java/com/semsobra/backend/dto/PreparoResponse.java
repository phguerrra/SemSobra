package com.semsobra.backend.dto;

public record PreparoResponse(
        Long id,
        String nome,
        String descricao,
        String unidadeMedida
) {
}
