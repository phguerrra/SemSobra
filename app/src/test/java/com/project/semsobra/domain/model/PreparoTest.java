package com.project.semsobra.domain.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class PreparoTest {

    @Test
    public void deveNormalizarOsDadosDoPreparo() {
        Preparo preparo = new Preparo("  ARROZ   BRANCO  ", null, "  kg  ", 2);

        assertEquals("Arroz branco", preparo.getNome());
        assertEquals("", preparo.getDescricao());
        assertEquals("kg", preparo.getUnidadeMedida());
        assertEquals(2, preparo.getDiaDaSemana());
    }

    @Test
    public void deveUsarKgQuandoAUnidadeNaoForInformada() {
        Preparo preparo = new Preparo("Feijão", "", " ", Preparo.TODOS_OS_DIAS);

        assertEquals("kg", preparo.getUnidadeMedida());
    }

    @Test
    public void deveNormalizarVariacoesDeEspacosEMaiusculasParaOMesmoNome() {
        Preparo primeiro = new Preparo("ARROZ   BRANCO", "", "kg", 1);
        Preparo segundo = new Preparo("  arroz branco  ", "", "kg", 2);

        assertEquals(primeiro.getNome(), segundo.getNome());
        assertEquals("Arroz branco", primeiro.getNome());
    }

    @Test
    public void deveRejeitarNomeVazio() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Preparo(" ", "", "kg", 1)
        );
    }

    @Test
    public void deveRejeitarDiaDaSemanaInvalido() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Preparo("Arroz", "", "kg", 8)
        );
    }
}
