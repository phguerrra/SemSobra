package com.project.semsobra.domain.usecase;

import com.project.semsobra.domain.repository.PreparoRepository;

public final class ValidarNomePreparoUseCase {

    private final PreparoRepository repository;

    public ValidarNomePreparoUseCase(PreparoRepository repository) {
        this.repository = repository;
    }

    public boolean estaDisponivel(String nome, int diaDaSemana, Long idAtual) {
        if (nome == null || nome.isBlank()) {
            return false;
        }

        String nomeNormalizado = com.project.semsobra.domain.model.Preparo.normalizarNome(nome);
        return !repository.existeNome(nomeNormalizado, diaDaSemana, idAtual);
    }
}
