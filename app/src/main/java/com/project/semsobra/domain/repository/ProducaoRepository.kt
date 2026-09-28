package com.project.semsobra.domain.repository

import com.project.semsobra.domain.model.ProducaoDia
import com.project.semsobra.domain.model.ItemProducao
import com.project.semsobra.domain.model.ProductionSummary
import kotlinx.coroutines.flow.Flow

interface ProducaoRepository {
    fun observarHistorico(): Flow<List<ProductionSummary>>

    fun salvarProducao(
        producao: ProducaoDia,
        quantidadesPorPreparo: Map<Long, Double>
    ): Long

    fun fecharProducao(
        producaoId: Long,
        clientesAtendidos: Int,
        itens: List<ItemProducao>
    )

    fun listarHistorico(): List<ProductionSummary>
}
