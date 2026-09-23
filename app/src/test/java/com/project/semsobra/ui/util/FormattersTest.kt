package com.project.semsobra.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FormattersTest {

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
