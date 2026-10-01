package com.project.semsobra.data.local.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProducaoDao {
    @Query(HISTORICO_QUERY)
    fun observarHistorico(): Flow<List<HistoricoRow>>

    @Query(HISTORICO_QUERY)
    fun listarHistorico(): List<HistoricoRow>

    @Query("SELECT id FROM producoes WHERE data = :data AND turno = :turno LIMIT 1")
    fun buscarId(data: String, turno: String): Long?

    @Insert
    fun inserirProducao(producao: ProducaoEntity): Long

    @Update
    fun atualizarProducao(producao: ProducaoEntity): Int

    @Query("DELETE FROM itens_producao WHERE producao_id = :producaoId")
    fun excluirItens(producaoId: Long)

    @Insert
    fun inserirItem(item: ItemProducaoEntity): Long

    @Query("UPDATE producoes SET clientes_atendidos = :clientes, fechada = 1 WHERE id = :id")
    fun fecharProducao(id: Long, clientes: Int): Int

    @Query("UPDATE producoes SET alterado_em = CURRENT_TIMESTAMP WHERE id = :id")
    fun registrarAlteracaoFechamento(id: Long): Int

    @Query("SELECT fechada FROM producoes WHERE id = :id LIMIT 1")
    fun estaFechada(id: Long): Boolean?

    @Query(
        "UPDATE itens_producao SET quantidade_produzida = :produzida, quantidade_sobra = :sobra, " +
            "acabou_antes_do_fim = :acabou, horario_acabou = :horario " +
            "WHERE id = :itemId AND producao_id = :producaoId"
    )
    fun atualizarFechamentoItem(
        itemId: Long,
        producaoId: Long,
        produzida: Double,
        sobra: Double,
        acabou: Boolean,
        horario: String?
    ): Int

    companion object {
        const val HISTORICO_QUERY = """
            SELECT p.id AS producao_id, p.data,
                p.dia_da_semana AS producao_dia_da_semana,
                p.clientes_atendidos, p.turno, p.restaurante_aberto, p.fechada, p.alterado_em,
                ip.id AS item_id, ip.preparo_id, ip.quantidade_produzida,
                ip.quantidade_sobra, ip.acabou_antes_do_fim, ip.horario_acabou,
                pr.nome, pr.descricao, pr.unidade_medida,
                pr.dia_da_semana AS preparo_dia_da_semana, pr.dias_semana_mask
            FROM producoes p
            INNER JOIN itens_producao ip ON ip.producao_id = p.id
            INNER JOIN preparos pr ON pr.id = ip.preparo_id
            ORDER BY p.data DESC, p.id DESC, ip.id ASC
        """
    }
}
