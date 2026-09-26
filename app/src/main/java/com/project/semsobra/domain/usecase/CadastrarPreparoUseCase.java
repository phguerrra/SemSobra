package com.project.semsobra.domain.usecase;

import com.project.semsobra.domain.model.Preparo;
import com.project.semsobra.domain.repository.PreparoRepository;

public final class CadastrarPreparoUseCase {

    public enum Resultado {
        CADASTRADO,
        NOME_DUPLICADO
    }

    private final PreparoRepository repository;
    private final ValidarNomePreparoUseCase validarNome;

    public CadastrarPreparoUseCase(
            PreparoRepository repository,
            ValidarNomePreparoUseCase validarNome
    ) {
        this.repository = repository;
        this.validarNome = validarNome;
    }

    public Resultado executar(
            String nome,
            String descricao,
            String unidadeMedida,
            int diaDaSemana
    ) {
        Preparo preparo = new Preparo(nome, descricao, unidadeMedida, diaDaSemana);
        if (!validarNome.estaDisponivel(preparo.getNome(), preparo.getDiaDaSemana(), null)) {
            return Resultado.NOME_DUPLICADO;
        }

        long id = repository.inserir(preparo);
        if (id <= 0) {
            throw new IllegalStateException("Não foi possível cadastrar o preparo");
        }
        return Resultado.CADASTRADO;
    }
}
