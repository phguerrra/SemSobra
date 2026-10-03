package com.project.semsobra.domain.exception;

public final class PreparoIndisponivelException extends IllegalStateException {

    public PreparoIndisponivelException() {
        super("Um preparo está inativo ou não existe mais. Atualize a lista antes de salvar a produção.");
    }
}
