package com.project.semsobra.data.repository

import android.content.Context
import com.project.semsobra.data.local.SemSobraDatabaseHelper
import com.project.semsobra.domain.previsao.model.Turno
import com.project.semsobra.ui.model.FoodUiModel
import com.project.semsobra.ui.model.ProductionDayUiModel
import com.project.semsobra.ui.model.ProductionItemDisplay
import com.project.semsobra.ui.model.ProductionItemUiModel
import com.project.semsobra.ui.model.ProductionSummary

class ProducaoLocalRepository(context: Context) {
    private val databaseHelper = SemSobraDatabaseHelper.getInstance(context)

    fun listarHistorico(): List<ProductionSummary> {
        val database = databaseHelper.readableDatabase
        val sql = """
            SELECT
                p.id AS producao_id,
                p.data,
                p.dia_da_semana AS producao_dia_da_semana,
                p.clientes_atendidos,
                p.turno,
                p.restaurante_aberto,
                p.fechada,
                ip.id AS item_id,
                ip.preparo_id,
                ip.quantidade_produzida,
                ip.quantidade_sobra,
                ip.acabou_antes_do_fim,
                ip.horario_acabou,
                pr.nome,
                pr.descricao,
                pr.unidade_medida,
                pr.dia_da_semana AS preparo_dia_da_semana
            FROM producoes p
            INNER JOIN itens_producao ip ON ip.producao_id = p.id
            INNER JOIN preparos pr ON pr.id = ip.preparo_id
            ORDER BY p.data DESC, p.id DESC, ip.id ASC
        """.trimIndent()

        val historico = linkedMapOf<Long, ResumoEmConstrucao>()
        database.rawQuery(sql, null).use { cursor ->
            while (cursor.moveToNext()) {
                val producaoId = cursor.getLong(cursor.getColumnIndexOrThrow("producao_id"))
                val resumo = historico.getOrPut(producaoId) {
                    ResumoEmConstrucao(
                        day = ProductionDayUiModel(
                            id = producaoId,
                            data = cursor.getString(cursor.getColumnIndexOrThrow("data")),
                            diaDaSemana = cursor.getInt(
                                cursor.getColumnIndexOrThrow("producao_dia_da_semana")
                            ),
                            clientesAtendidos = cursor.getInt(
                                cursor.getColumnIndexOrThrow("clientes_atendidos")
                            ),
                            turno = Turno.valueOf(
                                cursor.getString(cursor.getColumnIndexOrThrow("turno"))
                            ),
                            restauranteAberto = cursor.getInt(
                                cursor.getColumnIndexOrThrow("restaurante_aberto")
                            ) == 1
                        ),
                        fechado = cursor.getInt(cursor.getColumnIndexOrThrow("fechada")) == 1
                    )
                }

                val quantidadeProduzida = cursor.getDouble(
                    cursor.getColumnIndexOrThrow("quantidade_produzida")
                )
                val quantidadeSobra = cursor.getDouble(
                    cursor.getColumnIndexOrThrow("quantidade_sobra")
                )
                val preparoId = cursor.getLong(cursor.getColumnIndexOrThrow("preparo_id"))
                val item = ProductionItemUiModel(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow("item_id")),
                    producaoDiaId = producaoId,
                    alimentoId = preparoId,
                    quantidadeProduzida = quantidadeProduzida,
                    quantidadeSobra = quantidadeSobra,
                    acabouAntesDoFim = cursor.getInt(
                        cursor.getColumnIndexOrThrow("acabou_antes_do_fim")
                    ) == 1,
                    horarioAcabou = cursor.getString(
                        cursor.getColumnIndexOrThrow("horario_acabou")
                    )
                )
                val preparo = FoodUiModel(
                    id = preparoId,
                    nome = cursor.getString(cursor.getColumnIndexOrThrow("nome")),
                    descricao = cursor.getString(cursor.getColumnIndexOrThrow("descricao")),
                    unidadeMedida = cursor.getString(
                        cursor.getColumnIndexOrThrow("unidade_medida")
                    ),
                    diaDaSemana = cursor.getInt(
                        cursor.getColumnIndexOrThrow("preparo_dia_da_semana")
                    )
                )
                resumo.items += ProductionItemDisplay(
                    item = item,
                    food = preparo,
                    consumo = (quantidadeProduzida - quantidadeSobra).coerceAtLeast(0.0)
                )
            }
        }

        return historico.values.map { resumo ->
            ProductionSummary(
                day = resumo.day,
                items = resumo.items,
                totalSobra = resumo.items.sumOf { it.item.quantidadeSobra },
                fechado = resumo.fechado
            )
        }
    }

    private data class ResumoEmConstrucao(
        val day: ProductionDayUiModel,
        val fechado: Boolean,
        val items: MutableList<ProductionItemDisplay> = mutableListOf()
    )
}
