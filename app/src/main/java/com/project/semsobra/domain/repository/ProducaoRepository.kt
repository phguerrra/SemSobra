package com.project.semsobra.domain.repository

import com.project.semsobra.domain.model.ProductionDayUiModel
import com.project.semsobra.domain.model.ProductionItemUiModel
import com.project.semsobra.domain.model.ProductionSummary
import kotlinx.coroutines.flow.Flow

interface ProducaoRepository {
    fun observarHistorico(): Flow<List<ProductionSummary>>

    fun salvarProducao(
        producao: ProductionDayUiModel,
        quantidadesPorPreparo: Map<Long, Double>
    ): Long

    fun fecharProducao(
        producaoId: Long,
        clientesAtendidos: Int,
        itens: List<ProductionItemUiModel>
    )

    fun listarHistorico(): List<ProductionSummary>
}
