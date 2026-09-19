package com.project.semsobra.domain.usecase;

import com.project.semsobra.domain.repository.PreparoRepository;

public final class ValidarNomePreparoUseCase {

    private final PreparoRepository repository;

    public ValidarNomePreparoUseCase(PreparoRepository repository) {
        this.repository = repository;
    }

    public boolean estaDisponivel(String nome, Long idAtual) {
        if (nome == null || nome.isBlank()) {
            return false;
        }

        return !repository.existeNome(nome.trim(), idAtual);
    }
}
