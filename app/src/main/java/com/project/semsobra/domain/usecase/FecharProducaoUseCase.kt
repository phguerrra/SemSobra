package com.project.semsobra.domain.usecase

import com.project.semsobra.domain.model.ItemProducao
import com.project.semsobra.domain.model.ProductionSummary
import com.project.semsobra.domain.repository.ProducaoRepository

class FecharProducaoUseCase(
    private val repository: ProducaoRepository,
    private val validarDados: ValidarDadosProducaoUseCase = ValidarDadosProducaoUseCase()
) {
    fun executar(
        producaoId: Long,
        clientesAtendidos: Int,
        itens: List<ItemProducao>
    ): List<ProductionSummary> {
        require(producaoId > 0) { "A produção precisa ter um ID válido" }
        require(itens.isNotEmpty()) { "A produção precisa ter ao menos um item" }
        validarDados.validarClientesAtendidos(clientesAtendidos)
        itens.forEach { item ->
            validarDados.validarFechamentoDoItem(
                item.quantidadeProduzida,
                item.quantidadeSobra,
                item.acabouAntesDoFim,
                item.horarioAcabou
            )
        }

        repository.fecharProducao(producaoId, clientesAtendidos, itens)
        return repository.listarHistorico()
    }
}
