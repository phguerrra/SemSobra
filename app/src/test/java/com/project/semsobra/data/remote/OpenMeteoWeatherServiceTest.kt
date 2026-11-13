package com.project.semsobra.data.remote

import com.project.semsobra.domain.previsao.model.CondicaoClima
import org.junit.Assert.assertEquals
import org.junit.Test

class OpenMeteoWeatherServiceTest {

    private val service = OpenMeteoWeatherService()

    @Test
    fun `mapeia codigos WMO para condicoes climaticas corretas`() {
        assertEquals(CondicaoClima.ENSOLARADO, service.mapearWeatherCode(0))
        assertEquals(CondicaoClima.ENSOLARADO, service.mapearWeatherCode(1))
        assertEquals(CondicaoClima.NUBLADO, service.mapearWeatherCode(2))
        assertEquals(CondicaoClima.NUBLADO, service.mapearWeatherCode(3))
        assertEquals(CondicaoClima.CHUVOSO, service.mapearWeatherCode(61))
        assertEquals(CondicaoClima.CHUVOSO, service.mapearWeatherCode(80))
        assertEquals(CondicaoClima.TEMPESTADE, service.mapearWeatherCode(95))
        assertEquals(CondicaoClima.TEMPESTADE, service.mapearWeatherCode(99))
    }
}
