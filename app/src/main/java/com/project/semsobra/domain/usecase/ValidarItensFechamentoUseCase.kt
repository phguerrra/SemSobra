package com.project.semsobra.domain.usecase

import com.project.semsobra.domain.model.ItemProducao

class ValidarItensFechamentoUseCase {
    fun validarIdentificacao(producaoId: Long, itens: List<ItemProducao>) {
        require(producaoId > 0) { "A produção precisa ter um ID válido" }
        require(itens.isNotEmpty()) { "Informe todos os itens da produção para realizar o fechamento" }
    }

    fun executar(producaoId: Long, itens: List<ItemProducao>, preparosPorItem: Map<Long, Long>) {
        validarIdentificacao(producaoId, itens)
        require(itens.map { it.id }.toSet() == preparosPorItem.keys) {
            "Informe todos os itens atuais da produção. Atualize a tela antes de fechar."
        }
    }
}
