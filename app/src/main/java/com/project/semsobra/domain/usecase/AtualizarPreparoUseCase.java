package com.project.semsobra.domain.usecase;

import com.project.semsobra.domain.model.Preparo;
import com.project.semsobra.domain.repository.PreparoRepository;

import java.util.Optional;

public final class AtualizarPreparoUseCase {

    public enum Resultado {
        ATUALIZADO,
        NOME_DUPLICADO,
        NAO_ENCONTRADO
    }

    private final PreparoRepository repository;
    private final ValidarNomePreparoUseCase validarNome;

    public AtualizarPreparoUseCase(
            PreparoRepository repository,
            ValidarNomePreparoUseCase validarNome
    ) {
        this.repository = repository;
        this.validarNome = validarNome;
    }

    public Resultado executar(
            long id,
            String nome,
            String descricao,
            String unidadeMedida,
            int diaDaSemana
    ) {
        if (id <= 0) {
            return Resultado.NAO_ENCONTRADO;
        }

        Optional<Preparo> preparoAtual = repository.buscarPorId(id);
        if (preparoAtual.isEmpty()) {
            return Resultado.NAO_ENCONTRADO;
        }

        Preparo preparoAtualizado = new Preparo(
                id,
                nome,
                descricao,
                unidadeMedida,
                diaDaSemana,
                preparoAtual.get().getDiasSemanaMask()
        );
        if (!validarNome.estaDisponivel(
                preparoAtualizado.getNome(),
                preparoAtualizado.getDiaDaSemana(),
                id
        )) {
            return Resultado.NOME_DUPLICADO;
        }

        return repository.atualizar(preparoAtualizado)
                ? Resultado.ATUALIZADO
                : Resultado.NAO_ENCONTRADO;
    }
}
