package com.project.semsobra.data.local.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "preparos",
    indices = [Index(value = ["nome", "dia_da_semana"], unique = true)]
)
data class PreparoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(collate = ColumnInfo.NOCASE) val nome: String,
    val descricao: String = "",
    @ColumnInfo(name = "unidade_medida") val unidadeMedida: String,
    @ColumnInfo(name = "dia_da_semana") val diaDaSemana: Int,
    @ColumnInfo(name = "dias_semana_mask") val diasSemanaMask: Int = 0,
    val ativo: Boolean = true
)

@Entity(
    tableName = "producoes",
    indices = [Index(value = ["data", "turno"], unique = true)]
)
data class ProducaoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val data: String,
    @ColumnInfo(name = "dia_da_semana") val diaDaSemana: Int,
    @ColumnInfo(name = "clientes_atendidos") val clientesAtendidos: Int = 0,
    val turno: String,
    @ColumnInfo(name = "restaurante_aberto") val restauranteAberto: Boolean = true,
    val fechada: Boolean = false,
    @ColumnInfo(name = "alterado_em") val alteradoEm: String? = null
)

@Entity(
    tableName = "itens_producao",
    foreignKeys = [
        ForeignKey(
            entity = ProducaoEntity::class,
            parentColumns = ["id"],
            childColumns = ["producao_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PreparoEntity::class,
            parentColumns = ["id"],
            childColumns = ["preparo_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["producao_id", "preparo_id"], unique = true),
        Index(value = ["preparo_id"])
    ]
)
data class ItemProducaoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "producao_id") val producaoId: Long,
    @ColumnInfo(name = "preparo_id") val preparoId: Long,
    @ColumnInfo(name = "quantidade_produzida") val quantidadeProduzida: Double,
    @ColumnInfo(name = "quantidade_sobra") val quantidadeSobra: Double = 0.0,
    @ColumnInfo(name = "acabou_antes_do_fim") val acabouAntesDoFim: Boolean = false,
    @ColumnInfo(name = "horario_acabou") val horarioAcabou: String? = null
)

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
