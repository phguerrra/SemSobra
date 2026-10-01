package com.project.semsobra.data.local.room

import androidx.room.ColumnInfo

data class HistoricoRow(
    @ColumnInfo(name = "producao_id") val producaoId: Long,
    val data: String,
    @ColumnInfo(name = "producao_dia_da_semana") val producaoDiaDaSemana: Int,
    @ColumnInfo(name = "clientes_atendidos") val clientesAtendidos: Int,
    val turno: String,
    @ColumnInfo(name = "restaurante_aberto") val restauranteAberto: Boolean,
    val fechada: Boolean,
    @ColumnInfo(name = "alterado_em") val alteradoEm: String?,
    @ColumnInfo(name = "item_id") val itemId: Long,
    @ColumnInfo(name = "preparo_id") val preparoId: Long,
    @ColumnInfo(name = "quantidade_produzida") val quantidadeProduzida: Double,
    @ColumnInfo(name = "quantidade_sobra") val quantidadeSobra: Double,
    @ColumnInfo(name = "acabou_antes_do_fim") val acabouAntesDoFim: Boolean,
    @ColumnInfo(name = "horario_acabou") val horarioAcabou: String?,
    val nome: String,
    val descricao: String,
    @ColumnInfo(name = "unidade_medida") val unidadeMedida: String,
    @ColumnInfo(name = "preparo_dia_da_semana") val preparoDiaDaSemana: Int,
    @ColumnInfo(name = "dias_semana_mask") val diasSemanaMask: Int
)
