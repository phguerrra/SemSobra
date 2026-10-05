package com.project.semsobra.data.repository

import android.content.Context
import android.database.sqlite.SQLiteException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.project.semsobra.data.local.room.SemSobraDatabase
import com.project.semsobra.domain.model.ItemProducao
import com.project.semsobra.domain.model.Preparo
import com.project.semsobra.domain.model.ProducaoDia
import com.project.semsobra.domain.previsao.model.Turno
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FechamentoProducaoRepositoryTest {
    private lateinit var database: SemSobraDatabase
    private lateinit var producoes: ProducaoLocalRepository
    private var arrozId = 0L
    private var feijaoId = 0L

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            SemSobraDatabase::class.java
        ).allowMainThreadQueries().build()
        val preparos = PreparoLocalRepository(database.preparoDao())
        arrozId = preparos.inserir(Preparo("Arroz", "", "kg", 1))
        feijaoId = preparos.inserir(Preparo("Feijão", "", "kg", 1))
        producoes = ProducaoLocalRepository(database)
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun fechamentoIncompletoNaoAlteraProducaoNemItens() {
        val producaoId = criarProducao()
        assertFechamentoRejeitado(producaoId, itens(producaoId).take(1))
    }

    @Test
    fun itemRepetidoNaoSubstituiItemAusente() {
        val producaoId = criarProducao()
        val primeiro = itens(producaoId).first()
        assertFechamentoRejeitado(producaoId, listOf(primeiro, primeiro.copy(quantidadeSobra = 1.0)))
    }

    @Test
    fun itemRealDeOutraProducaoNaoPodeSerIncluidoMesmoComVinculoAlterado() {
        val producaoId = criarProducao()
        val outraProducaoId = criarProducao(Turno.JANTAR)
        val outroItem = itens(outraProducaoId).last().copy(producaoDiaId = producaoId)
        assertFechamentoRejeitado(producaoId, listOf(itens(producaoId).first(), outroItem))
    }

    @Test
    fun itemComProducaoDeclaradaIncorretaNaoAlteraDados() {
        val producaoId = criarProducao()
        val lista = itens(producaoId)
        assertFechamentoRejeitado(producaoId, listOf(lista.first().copy(producaoDiaId = Long.MAX_VALUE), lista.last()))
    }

    @Test
    fun preparoIncorretoParaItemExistenteNaoAlteraDados() {
        val producaoId = criarProducao()
        val lista = itens(producaoId)
        assertFechamentoRejeitado(producaoId, listOf(lista.first().copy(alimentoId = feijaoId), lista.last()))
    }

    @Test
    fun itensAntigosDepoisDeEditarProducaoNaoPodemSerFechados() {
        val producaoId = criarProducao()
        val itensAntigos = itens(producaoId)
        criarProducao()
        assertFechamentoRejeitado(producaoId, itensAntigos)
    }

    @Test
    fun correcaoIncompletaPreservaFechamentoAnteriorInclusiveDataDaAlteracao() {
        val producaoId = criarProducao()
        val lista = itens(producaoId).map { it.copy(quantidadeSobra = 1.0) }
        producoes.fecharProducao(producaoId, 100, lista)
        producoes.fecharProducao(producaoId, 110, lista)

        assertFechamentoRejeitado(producaoId, lista.take(1))
        assertTrue(producoes.listarHistorico().single().fechado)
    }

    @Test
    fun listaCompletaEmOutraOrdemPermiteFechamentoECorrecao() {
        val producaoId = criarProducao()
        val lista = itens(producaoId).reversed().map { it.copy(quantidadeSobra = 1.0) }
        producoes.fecharProducao(producaoId, 100, lista)
        val fechado = producoes.listarHistorico().single()
        assertTrue(fechado.fechado)
        assertEquals(100, fechado.day.clientesAtendidos)
        assertTrue(fechado.items.all { it.item.quantidadeSobra == 1.0 })

        producoes.fecharProducao(producaoId, 120, lista.map { it.copy(quantidadeSobra = 2.0) })

        val corrigido = producoes.listarHistorico().single()
        assertTrue(corrigido.fechado)
        assertEquals(120, corrigido.day.clientesAtendidos)
        assertTrue(corrigido.items.all { it.item.quantidadeSobra == 2.0 })
        assertTrue(!corrigido.day.alteradoEm.isNullOrBlank())
    }

    @Test
    fun falhaDuranteAtualizacaoDoSegundoItemDesfazTodasAsAlteracoes() {
        val producaoId = criarProducao()
        val lista = itens(producaoId)
        val historicoAntes = database.producaoDao().listarHistorico()
        database.openHelper.writableDatabase.execSQL(
            "CREATE TEMP TRIGGER falhar_segundo_item BEFORE UPDATE ON itens_producao " +
                "WHEN OLD.id = ${lista.last().id} " +
                "BEGIN SELECT RAISE(ABORT, 'Falha simulada no segundo item'); END"
        )

        assertThrows(SQLiteException::class.java) {
            producoes.fecharProducao(producaoId, 999, lista.map { it.copy(quantidadeSobra = 1.0) })
        }

        assertEquals(historicoAntes, database.producaoDao().listarHistorico())
    }

    private fun criarProducao(turno: Turno = Turno.ALMOCO): Long = producoes.salvarProducao(
        ProducaoDia(data = "2026-10-04", diaDaSemana = 7, turno = turno),
        linkedMapOf(arrozId to 10.0, feijaoId to 5.0)
    )

    private fun itens(producaoId: Long): List<ItemProducao> =
        producoes.listarHistorico().single { it.day.id == producaoId }.items.map { it.item }

    private fun assertFechamentoRejeitado(producaoId: Long, lista: List<ItemProducao>) {
        val historicoAntes = database.producaoDao().listarHistorico()
        assertThrows(IllegalArgumentException::class.java) {
            producoes.fecharProducao(producaoId, 999, lista)
        }
        assertEquals(historicoAntes, database.producaoDao().listarHistorico())
    }
}
