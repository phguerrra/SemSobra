package com.project.semsobra.domain.usecase

import com.project.semsobra.domain.model.ItemProducao

class ValidarItensFechamentoUseCase {
    fun validarIdentificacao(producaoId: Long, itens: List<ItemProducao>) {
        require(producaoId > 0) { "A produção precisa ter um ID válido" }
        require(itens.isNotEmpty()) { "Informe todos os itens da produção para realizar o fechamento" }
        require(itens.all { it.id > 0 && it.alimentoId > 0 }) {
            "Os itens do fechamento precisam ter IDs válidos"
        }
        require(itens.map { it.id }.toSet().size == itens.size) {
            "O fechamento não pode conter itens repetidos"
        }
        require(itens.all { it.producaoDiaId == producaoId }) {
            "O fechamento contém um item de outra produção"
        }
    }

    fun executar(producaoId: Long, itens: List<ItemProducao>, preparosPorItem: Map<Long, Long>) {
        validarIdentificacao(producaoId, itens)
        require(itens.map { it.id }.toSet() == preparosPorItem.keys) {
            "Informe todos os itens atuais da produção. Atualize a tela antes de fechar."
        }
        require(itens.all { preparosPorItem[it.id] == it.alimentoId }) {
            "Um item do fechamento não corresponde ao preparo salvo na produção"
        }
    }
}
