package com.project.semsobra.domain.exception;

public final class ProducaoFechadaException extends IllegalStateException {

    public ProducaoFechadaException() {
        super("A produção já está fechada");
    }
}
