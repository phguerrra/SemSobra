package com.project.semsobra.domain.usecase;

import java.util.regex.Pattern;

public final class ValidarDadosProducaoUseCase {

    public static final int MAXIMO_CLIENTES = 10_000;
    public static final double MAXIMA_QUANTIDADE = 10_000.0;
    private static final Pattern FORMATO_HORARIO = Pattern.compile("(?:[01]\\d|2[0-3]):[0-5]\\d");

    public void validarQuantidadeProduzida(double quantidade) {
        validarNumeroFinito(quantidade, "A quantidade produzida");
        if (quantidade <= 0.0) {
            throw new IllegalArgumentException("Informe uma quantidade produzida maior que zero");
        }
        if (quantidade > MAXIMA_QUANTIDADE) {
            throw new IllegalArgumentException(
                    "A quantidade produzida não pode ultrapassar " + MAXIMA_QUANTIDADE
            );
        }
    }

    public void validarClientesAtendidos(int clientesAtendidos) {
        if (clientesAtendidos <= 0) {
            throw new IllegalArgumentException("Informe a quantidade de clientes atendidos");
        }
        if (clientesAtendidos > MAXIMO_CLIENTES) {
            throw new IllegalArgumentException(
                    "A quantidade de clientes não pode ultrapassar " + MAXIMO_CLIENTES
            );
        }
    }

    public void validarFechamentoDoItem(
            double quantidadeProduzida,
            double quantidadeSobra,
            boolean acabouAntesDoFim,
            String horarioAcabou
    ) {
        validarQuantidadeProduzida(quantidadeProduzida);
        validarNumeroFinito(quantidadeSobra, "A quantidade de sobra");
        if (quantidadeSobra < 0.0) {
            throw new IllegalArgumentException("A quantidade de sobra não pode ser negativa");
        }
        if (quantidadeSobra > quantidadeProduzida) {
            throw new IllegalArgumentException("A sobra não pode ser maior que a quantidade produzida");
        }
        if (quantidadeSobra > MAXIMA_QUANTIDADE) {
            throw new IllegalArgumentException(
                    "A quantidade de sobra não pode ultrapassar " + MAXIMA_QUANTIDADE
            );
        }

        validarHorario(acabouAntesDoFim, horarioAcabou);
    }

    private void validarHorario(boolean acabouAntesDoFim, String horarioAcabou) {
        if (!acabouAntesDoFim) {
            return;
        }

        String horario = horarioAcabou == null ? "" : horarioAcabou.trim();
        if (horario.isEmpty()) {
            throw new IllegalArgumentException("Informe o horário em que o preparo acabou");
        }
        if (!FORMATO_HORARIO.matcher(horario).matches()) {
            throw new IllegalArgumentException("Informe o horário no formato HH:mm");
        }
    }

    private void validarNumeroFinito(double valor, String nomeDoCampo) {
        if (!Double.isFinite(valor)) {
            throw new IllegalArgumentException(nomeDoCampo + " precisa ser um número válido");
        }
    }
}
