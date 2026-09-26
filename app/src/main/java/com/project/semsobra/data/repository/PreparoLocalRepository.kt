package com.project.semsobra.data.repository

import android.content.Context
import com.project.semsobra.data.local.room.PreparoDao
import com.project.semsobra.data.local.room.PreparoEntity
import com.project.semsobra.data.local.room.SemSobraDatabase
import com.project.semsobra.domain.model.Preparo
import com.project.semsobra.domain.repository.PreparoRepository
import java.util.Optional
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PreparoLocalRepository(context: Context) : PreparoRepository {
    private val dao: PreparoDao = SemSobraDatabase.getInstance(context).preparoDao()

    fun observarTodos(): Flow<List<Preparo>> = dao.observarAtivos().map { entities ->
        entities.map { it.toDomain() }
    }

    override fun listarTodos(): List<Preparo> = dao.listarAtivos().map { it.toDomain() }

    override fun buscarPorId(id: Long): Optional<Preparo> =
        Optional.ofNullable(dao.buscarPorId(id)?.toDomain())

    override fun inserir(preparo: Preparo): Long = dao.inserir(preparo.toEntity())

    override fun atualizar(preparo: Preparo): Boolean {
        require(preparo.id > 0) { "O preparo precisa ter um ID para ser atualizado" }
        val atual = dao.buscarPorId(preparo.id) ?: return false
        return dao.atualizar(preparo.toEntity(ativo = atual.ativo)) > 0
    }

    override fun excluir(id: Long): Boolean = dao.excluir(id) > 0

    override fun inativar(id: Long): Boolean = dao.inativar(id) > 0

    override fun atualizarDias(id: Long, diasSemanaMask: Int): Boolean =
        dao.atualizarDias(id, diasSemanaMask) > 0

    override fun existeNome(nome: String, diaDaSemana: Int, idIgnorado: Long?): Boolean =
        dao.existeNome(Preparo.normalizarNome(nome), diaDaSemana, idIgnorado)

    override fun estaEmUsoNoHistorico(id: Long): Boolean = dao.estaEmUsoNoHistorico(id)

    private fun PreparoEntity.toDomain() = Preparo(
        id,
        nome,
        descricao,
        unidadeMedida,
        diaDaSemana,
        diasSemanaMask
    )

    private fun Preparo.toEntity(ativo: Boolean = true) = PreparoEntity(
        id = id,
        nome = nome,
        descricao = descricao,
        unidadeMedida = unidadeMedida,
        diaDaSemana = diaDaSemana,
        diasSemanaMask = diasSemanaMask,
        ativo = ativo
    )
}
