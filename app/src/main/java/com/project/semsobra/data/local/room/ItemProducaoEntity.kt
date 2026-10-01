package com.project.semsobra.data.local.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

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
