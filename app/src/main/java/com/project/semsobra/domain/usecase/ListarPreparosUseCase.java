package com.project.semsobra.domain.usecase;

import com.project.semsobra.domain.model.Preparo;
import com.project.semsobra.domain.repository.PreparoRepository;

import java.util.List;

public final class ListarPreparosUseCase {

    private final PreparoRepository repository;

    public ListarPreparosUseCase(PreparoRepository repository) {
        this.repository = repository;
    }

    public List<Preparo> executar() {
        return repository.listarTodos();
    }
}
