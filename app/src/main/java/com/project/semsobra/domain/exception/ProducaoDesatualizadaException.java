package com.project.semsobra.domain.exception;

public final class ProducaoDesatualizadaException extends IllegalArgumentException {

    public ProducaoDesatualizadaException() {
        super("A quantidade produzida foi alterada. Atualize os dados antes de fechar a produção.");
    }
}
