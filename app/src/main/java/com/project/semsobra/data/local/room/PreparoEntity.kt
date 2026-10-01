package com.project.semsobra.data.local.room

import androidx.room.ColumnInfo
import androidx.room.Entity
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
