package com.project.semsobra.domain.previsao

import com.project.semsobra.domain.previsao.model.EntradaPrevisao
import com.project.semsobra.domain.previsao.model.ResultadoPrevisao

interface MotorPrevisao {
    fun calcular(entrada: EntradaPrevisao): ResultadoPrevisao
}
