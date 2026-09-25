package com.project.semsobra.data.repository

import android.content.Context
import com.project.semsobra.data.local.room.HistoricoRow
import com.project.semsobra.data.local.room.ItemProducaoEntity
import com.project.semsobra.data.local.room.ProducaoEntity
import com.project.semsobra.data.local.room.SemSobraDatabase
import com.project.semsobra.domain.model.Preparo
import com.project.semsobra.domain.model.ProductionDayUiModel
import com.project.semsobra.domain.model.ProductionItemDisplay
import com.project.semsobra.domain.model.ProductionItemUiModel
import com.project.semsobra.domain.model.ProductionSummary
import com.project.semsobra.domain.model.QuantityPolicy
import com.project.semsobra.domain.previsao.model.Turno
import com.project.semsobra.domain.repository.ProducaoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProducaoLocalRepository(context: Context) : ProducaoRepository {
    private val database = SemSobraDatabase.getInstance(context)
    private val dao = database.producaoDao()

    override fun observarHistorico(): Flow<List<ProductionSummary>> =
        dao.observarHistorico().map(::mapearHistorico)

    override fun salvarProducao(
        producao: ProductionDayUiModel,
        quantidadesPorPreparo: Map<Long, Double>
    ): Long {
        require(quantidadesPorPreparo.isNotEmpty()) {
            "A produção precisa ter ao menos um item"
        }
        require(quantidadesPorPreparo.all { (preparoId, quantidade) ->
            preparoId > 0 && quantidade > 0.0
        }) { "Os preparos e as quantidades da produção precisam ser válidos" }

        var resultado = 0L
        database.runInTransaction {
            val existenteId = dao.buscarId(producao.data, producao.turno.name)
            val entity = producao.toEntity(id = existenteId ?: 0L)
            val producaoId = existenteId ?: dao.inserirProducao(entity)
            if (existenteId != null) dao.atualizarProducao(entity)

            dao.excluirItens(producaoId)
            quantidadesPorPreparo.forEach { (preparoId, quantidade) ->
                dao.inserirItem(
                    ItemProducaoEntity(
                        producaoId = producaoId,
                        preparoId = preparoId,
                        quantidadeProduzida = QuantityPolicy.normalize(quantidade)
                    )
                )
            }
            resultado = producaoId
        }
        return resultado
    }

    override fun fecharProducao(
        producaoId: Long,
        clientesAtendidos: Int,
        itens: List<ProductionItemUiModel>
    ) {
        require(producaoId > 0) { "A produção precisa ter um ID válido" }
        require(clientesAtendidos > 0) { "Informe os clientes atendidos" }
        require(itens.isNotEmpty()) { "A produção precisa ter ao menos um item" }

        database.runInTransaction {
            check(dao.fecharProducao(producaoId, clientesAtendidos) == 1) {
                "Produção não encontrada"
            }
            itens.forEach { item ->
                val horario = item.horarioAcabou
                    ?.trim()
                    ?.takeIf { item.acabouAntesDoFim && it.isNotEmpty() }
                check(
                    dao.atualizarFechamentoItem(
                        itemId = item.id,
                        producaoId = producaoId,
                        sobra = QuantityPolicy.normalize(item.quantidadeSobra),
                        acabou = item.acabouAntesDoFim,
                        horario = horario
                    ) == 1
                ) { "Item da produção não encontrado" }
            }
        }
    }

    override fun listarHistorico(): List<ProductionSummary> = mapearHistorico(dao.listarHistorico())

    private fun mapearHistorico(rows: List<HistoricoRow>): List<ProductionSummary> = rows
        .groupBy(HistoricoRow::producaoId)
        .values
        .map { productionRows ->
            val first = productionRows.first()
            ProductionSummary(
                day = ProductionDayUiModel(
                    id = first.producaoId,
                    data = first.data,
                    diaDaSemana = first.producaoDiaDaSemana,
                    clientesAtendidos = first.clientesAtendidos,
                    turno = Turno.valueOf(first.turno),
                    restauranteAberto = first.restauranteAberto
                ),
                items = productionRows.map(::mapearItem),
                fechado = first.fechada
            )
        }
        .sortedWith(compareByDescending<ProductionSummary> { it.day.data }.thenByDescending { it.day.id })

    private fun mapearItem(row: HistoricoRow): ProductionItemDisplay {
        val produced = QuantityPolicy.normalize(row.quantidadeProduzida)
        val leftover = QuantityPolicy.normalize(row.quantidadeSobra)
        val item = ProductionItemUiModel(
            id = row.itemId,
            producaoDiaId = row.producaoId,
            alimentoId = row.preparoId,
            quantidadeProduzida = produced,
            quantidadeSobra = leftover,
            acabouAntesDoFim = row.acabouAntesDoFim,
            horarioAcabou = row.horarioAcabou
        )
        val food = Preparo(
            row.preparoId,
            row.nome,
            row.descricao,
            row.unidadeMedida,
            row.preparoDiaDaSemana
        )
        return ProductionItemDisplay(
            item = item,
            food = food,
            consumo = QuantityPolicy.subtract(produced, leftover).coerceAtLeast(0.0)
        )
    }

    private fun ProductionDayUiModel.toEntity(id: Long) = ProducaoEntity(
        id = id,
        data = data,
        diaDaSemana = diaDaSemana,
        clientesAtendidos = clientesAtendidos,
        turno = turno.name,
        restauranteAberto = restauranteAberto,
        fechada = false
    )
}
