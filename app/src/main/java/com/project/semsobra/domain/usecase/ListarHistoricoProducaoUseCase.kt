package com.project.semsobra.domain.usecase

import com.project.semsobra.domain.model.ProductionSummary
import com.project.semsobra.domain.repository.ProducaoRepository

class ListarHistoricoProducaoUseCase(
    private val repository: ProducaoRepository
) {
    fun executar(): List<ProductionSummary> = repository.listarHistorico()
}
