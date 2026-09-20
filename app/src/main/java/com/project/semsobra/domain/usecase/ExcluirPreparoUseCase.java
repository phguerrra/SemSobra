package com.project.semsobra.domain.usecase;

import com.project.semsobra.domain.repository.PreparoRepository;

public final class ExcluirPreparoUseCase {

    public enum Resultado {
        EXCLUIDO,
        INATIVADO_POR_HISTORICO,
        NAO_ENCONTRADO
    }

    private final PreparoRepository repository;

    public ExcluirPreparoUseCase(PreparoRepository repository) {
        this.repository = repository;
    }

    public Resultado executar(long preparoId) {
        if (preparoId <= 0) {
            return Resultado.NAO_ENCONTRADO;
        }
        if (repository.estaEmUsoNoHistorico(preparoId)) {
            return repository.inativar(preparoId)
                    ? Resultado.INATIVADO_POR_HISTORICO
                    : Resultado.NAO_ENCONTRADO;
        }
        return repository.excluir(preparoId)
                ? Resultado.EXCLUIDO
                : Resultado.NAO_ENCONTRADO;
    }
}
