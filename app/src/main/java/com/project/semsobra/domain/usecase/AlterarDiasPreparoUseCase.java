package com.project.semsobra.domain.usecase;

import com.project.semsobra.domain.model.Preparo;
import com.project.semsobra.domain.repository.PreparoRepository;

import java.util.Optional;

public final class AlterarDiasPreparoUseCase {

    private static final int PRIMEIRO_DIA = 1;
    private static final int ULTIMO_DIA = 7;
    private static final int MASCARA_DIAS_VALIDOS = 0b1111111;

    public enum Status {
        ATUALIZADO,
        NAO_ENCONTRADO
    }

    public static final class Resultado {
        private final Status status;
        private final int diasSemanaMask;

        private Resultado(Status status, int diasSemanaMask) {
            this.status = status;
            this.diasSemanaMask = diasSemanaMask;
        }

        public Status getStatus() {
            return status;
        }

        public int getDiasSemanaMask() {
            return diasSemanaMask;
        }
    }

    private final PreparoRepository repository;

    public AlterarDiasPreparoUseCase(PreparoRepository repository) {
        this.repository = repository;
    }

    public Resultado executar(long preparoId, int dia, boolean selecionado) {
        if (dia < PRIMEIRO_DIA || dia > ULTIMO_DIA) {
            throw new IllegalArgumentException("O dia da semana deve estar entre 1 e 7");
        }
        if (preparoId <= 0) {
            return new Resultado(Status.NAO_ENCONTRADO, 0);
        }

        Optional<Preparo> preparo = repository.buscarPorId(preparoId);
        if (preparo.isEmpty()) {
            return new Resultado(Status.NAO_ENCONTRADO, 0);
        }

        int mascaraAtual = preparo.get().getDiasSemanaMask() & MASCARA_DIAS_VALIDOS;
        int bitDoDia = 1 << (dia - 1);
        int novaMascara = selecionado
                ? mascaraAtual | bitDoDia
                : mascaraAtual & ~bitDoDia;

        if (!repository.atualizarDias(preparoId, novaMascara)) {
            return new Resultado(Status.NAO_ENCONTRADO, mascaraAtual);
        }
        return new Resultado(Status.ATUALIZADO, novaMascara);
    }
}
