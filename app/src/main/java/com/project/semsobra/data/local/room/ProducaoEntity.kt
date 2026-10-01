package com.project.semsobra.data.local.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

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
