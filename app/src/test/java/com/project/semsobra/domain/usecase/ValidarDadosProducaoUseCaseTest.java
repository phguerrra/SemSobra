package com.project.semsobra.domain.usecase;

import org.junit.Test;

import static org.junit.Assert.assertThrows;

public class ValidarDadosProducaoUseCaseTest {

    private final ValidarDadosProducaoUseCase useCase = new ValidarDadosProducaoUseCase();

    @Test
    public void deveAceitarQuantidadeProduzidaValida() {
        useCase.validarQuantidadeProduzida(25.5);
    }

    @Test
    public void deveRejeitarQuantidadeNaoNumerica() {
        assertThrows(
                IllegalArgumentException.class,
                () -> useCase.validarQuantidadeProduzida(Double.NaN)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> useCase.validarQuantidadeProduzida(Double.POSITIVE_INFINITY)
        );
    }

    @Test
    public void deveRejeitarQuantidadeZeroOuNegativa() {
        assertThrows(
                IllegalArgumentException.class,
                () -> useCase.validarQuantidadeProduzida(0.0)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> useCase.validarQuantidadeProduzida(-1.0)
        );
    }

    @Test
    public void deveRejeitarQuantidadeAcimaDoLimite() {
        assertThrows(
                IllegalArgumentException.class,
                () -> useCase.validarQuantidadeProduzida(
                        ValidarDadosProducaoUseCase.MAXIMA_QUANTIDADE + 1.0
                )
        );
    }

    @Test
    public void deveAceitarClientesDentroDoLimite() {
        useCase.validarClientesAtendidos(150);
    }

    @Test
    public void deveRejeitarClientesForaDoLimite() {
        assertThrows(
                IllegalArgumentException.class,
                () -> useCase.validarClientesAtendidos(0)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> useCase.validarClientesAtendidos(
                        ValidarDadosProducaoUseCase.MAXIMO_CLIENTES + 1
                )
        );
    }

    @Test
    public void deveAceitarFechamentoValido() {
        useCase.validarFechamentoDoItem(20.0, 2.5, true, "13:40");
        useCase.validarFechamentoDoItem(20.0, 0.0, false, null);
    }

    @Test
    public void deveRejeitarSobraInvalida() {
        assertThrows(
                IllegalArgumentException.class,
                () -> useCase.validarFechamentoDoItem(20.0, -1.0, false, null)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> useCase.validarFechamentoDoItem(20.0, 21.0, false, null)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> useCase.validarFechamentoDoItem(20.0, Double.NaN, false, null)
        );
    }

    @Test
    public void deveExigirHorarioQuandoPreparoAcabou() {
        assertThrows(
                IllegalArgumentException.class,
                () -> useCase.validarFechamentoDoItem(20.0, 0.0, true, null)
        );
    }

    @Test
    public void deveRejeitarHorarioForaDoFormato() {
        assertThrows(
                IllegalArgumentException.class,
                () -> useCase.validarFechamentoDoItem(20.0, 0.0, true, "25:70")
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> useCase.validarFechamentoDoItem(20.0, 0.0, true, "9:30")
        );
    }
}
