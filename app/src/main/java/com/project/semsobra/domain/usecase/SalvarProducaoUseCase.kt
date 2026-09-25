package com.project.semsobra.domain.usecase

import com.project.semsobra.domain.model.ProductionDayUiModel
import com.project.semsobra.domain.model.ProductionSummary
import com.project.semsobra.domain.repository.ProducaoRepository

class SalvarProducaoUseCase(
    private val repository: ProducaoRepository,
    private val validarDados: ValidarDadosProducaoUseCase = ValidarDadosProducaoUseCase()
) {
    fun executar(
        producao: ProductionDayUiModel,
        quantidadesPorPreparo: Map<Long, Double>
    ): List<ProductionSummary> {
        require(quantidadesPorPreparo.isNotEmpty()) {
            "Informe ao menos uma quantidade maior que zero"
        }
        require(quantidadesPorPreparo.keys.all { it > 0 }) {
            "Os preparos informados precisam ser válidos"
        }
        quantidadesPorPreparo.values.forEach(validarDados::validarQuantidadeProduzida)

        repository.salvarProducao(producao, quantidadesPorPreparo)
        return repository.listarHistorico()
    }
}
