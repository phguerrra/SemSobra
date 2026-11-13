package com.project.semsobra.data.remote

import com.project.semsobra.domain.previsao.model.CondicaoClima
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class OpenMeteoWeatherService {

    suspend fun buscarPrevisaoClima(
        latitude: Double = -23.5505,
        longitude: Double = -46.6333,
        data: LocalDate = LocalDate.now().plusDays(1)
    ): CondicaoClima = withContext(Dispatchers.IO) {
        runCatching {
            val urlString = "https://api.open-meteo.com/v1/forecast" +
                "?latitude=$latitude" +
                "&longitude=$longitude" +
                "&daily=weather_code" +
                "&timezone=auto" +
                "&start_date=$data" +
                "&end_date=$data"

            val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4000
                readTimeout = 4000
            }

            if (connection.responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)
                val daily = json.optJSONObject("daily")
                val weatherCodes = daily?.optJSONArray("weather_code")
                if (weatherCodes != null && weatherCodes.length() > 0) {
                    val code = weatherCodes.getInt(0)
                    mapearWeatherCode(code)
                } else {
                    CondicaoClima.ENSOLARADO
                }
            } else {
                CondicaoClima.ENSOLARADO
            }
        }.getOrDefault(CondicaoClima.ENSOLARADO)
    }

    internal fun mapearWeatherCode(code: Int): CondicaoClima = when (code) {
        0, 1 -> CondicaoClima.ENSOLARADO
        2, 3, 45, 48 -> CondicaoClima.NUBLADO
        51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> CondicaoClima.CHUVOSO
        95, 96, 99 -> CondicaoClima.TEMPESTADE
        else -> if (code in 50..89) CondicaoClima.CHUVOSO else CondicaoClima.ENSOLARADO
    }
}
