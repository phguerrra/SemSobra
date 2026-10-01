package com.project.semsobra.data.local.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PreparoDao {
    @Query("SELECT * FROM preparos WHERE ativo = 1 ORDER BY nome COLLATE NOCASE ASC")
    fun observarAtivos(): Flow<List<PreparoEntity>>

    @Query("SELECT * FROM preparos WHERE ativo = 1 ORDER BY nome COLLATE NOCASE ASC")
    fun listarAtivos(): List<PreparoEntity>

    @Query("SELECT * FROM preparos WHERE id = :id LIMIT 1")
    fun buscarPorId(id: Long): PreparoEntity?

    @Insert
    fun inserir(preparo: PreparoEntity): Long

    @Update
    fun atualizar(preparo: PreparoEntity): Int

    @Query("DELETE FROM preparos WHERE id = :id")
    fun excluir(id: Long): Int

    @Query("UPDATE preparos SET ativo = 0 WHERE id = :id")
    fun inativar(id: Long): Int

    @Query("UPDATE preparos SET dias_semana_mask = :mask WHERE id = :id")
    fun atualizarDias(id: Long, mask: Int): Int

    @Query(
        "SELECT EXISTS(SELECT 1 FROM preparos " +
            "WHERE nome = :nome COLLATE NOCASE AND dia_da_semana = :dia " +
            "AND (:idIgnorado IS NULL OR id <> :idIgnorado))"
    )
    fun existeNome(nome: String, dia: Int, idIgnorado: Long?): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM itens_producao WHERE preparo_id = :id)")
    fun estaEmUsoNoHistorico(id: Long): Boolean
}
