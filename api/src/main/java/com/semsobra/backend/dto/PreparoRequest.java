package com.semsobra.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PreparoRequest (

        @NotBlank(message = "O nome é obrigatorio")
        @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres")
        String nome,

        @Size(max = 500, message = "A descrição deve ter no máximo 500 caracteres")
        String descricao,

        @NotBlank(message = "A unidade de medida é obrigatória")
        @Size(max = 20, message = "A unidade deve ter no máximo 20 caracteres")
        String unidadeMedida
) {
}
