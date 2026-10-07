package com.project.semsobra.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class QuantityPolicyTest {
    @Test
    fun `respeita o limite entre zero e a menor quantidade positiva`() {
        assertEquals(0.0, QuantityPolicy.normalize(0.0001), 0.0)
        assertEquals(0.0, QuantityPolicy.normalize(Math.nextDown(0.0005)), 0.0)
        assertEquals(0.001, QuantityPolicy.normalize(0.0005), 0.0)
        assertEquals(0.001, QuantityPolicy.normalize(0.001), 0.0)
    }

    @Test
    fun `normaliza quantidade para tres casas decimais`() {
        assertEquals(1.235, QuantityPolicy.normalize(1.2345), 0.0)
    }

    @Test
    fun `soma quantidades sem acumular imprecisao binaria`() {
        assertEquals(0.3, QuantityPolicy.sum(listOf(0.1, 0.2)), 0.0)
    }

    @Test
    fun `subtrai quantidades usando a precisao padrao`() {
        assertEquals(0.2, QuantityPolicy.subtract(0.3, 0.1), 0.0)
    }
}
