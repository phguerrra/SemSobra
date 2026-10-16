package com.project.semsobra.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FormattersTest {

    @Test
    fun `permite apagar o campo e digitar decimais em etapas`() {
        listOf("", ",", ".", "0,", "0.", "12,", "12.", ",5", ".5").forEach {
            assertTrue("Deveria permitir a digitação de '$it'", isDecimalInput(it))
        }
        assertNull(parseDoubleOrNull(","))
        assertNull(parseDoubleOrNull("."))
    }

    @Test
    fun `aceita quantidades com ponto ou virgula sem perder precisao ao digitar`() {
        listOf("0", "12", "12,5", "12.5", "0,0001", "10000,1234").forEach {
            assertTrue("Deveria aceitar '$it'", isDecimalInput(it))
        }
        assertEquals(0.5, parseDoubleOrNull(",5")!!, 0.0)
        assertEquals(0.5, parseDoubleOrNull(".5")!!, 0.0)
    }

    @Test
    fun `recusa sinais letras espacos e valores nao finitos na digitacao decimal`() {
        listOf("-5", "+5", "1e3", "NaN", "Infinity", "8kg", " 8", "8 ", "1 000", "１２").forEach {
            assertFalse("Deveria recusar '$it' sem alterar seu significado", isDecimalInput(it))
        }
    }

    @Test
    fun `recusa mais de um separador decimal inclusive na colagem`() {
        listOf("1,2,3", "1.2.3", "1,2.3", "1.2,3", "1.000,5", "1,000.5").forEach {
            assertFalse("Deveria recusar '$it'", isDecimalInput(it))
        }
    }

    @Test
    fun `permite apagar e digitar apenas algarismos no campo de clientes`() {
        listOf("", "0", "1", "10000", "10001").forEach {
            assertTrue("Deveria permitir a digitação de '$it'", isIntegerInput(it))
        }
    }

    @Test
    fun `recusa decimais sinais e caracteres invalidos no campo de clientes`() {
        listOf("1,5", "1.5", "-5", "+5", "1e3", "abc", " 5", "5 ", "１２").forEach {
            assertFalse("Deveria recusar '$it'", isIntegerInput(it))
        }
    }

    @Test
    fun `converte ponto e virgula decimal`() {
        assertEquals(12.5, parseDoubleOrNull("12.5")!!, 0.0)
        assertEquals(12.5, parseDoubleOrNull("12,5")!!, 0.0)
    }

    @Test
    fun `nao converte entrada invalida para zero`() {
        assertNull(parseDoubleOrNull("valor inválido"))
        assertNull(parseDoubleOrNull(""))
    }

    @Test
    fun `rejeita valores nao finitos`() {
        assertNull(parseDoubleOrNull("NaN"))
        assertNull(parseDoubleOrNull("Infinity"))
    }
}
