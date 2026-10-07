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
import com.project.semsobra.domain.usecase.ValidarDadosProducaoUseCase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

    @Test
    fun clientesInvalidosNaoAlteramProducaoNemItens() {
        val producaoId = criarProducao()
        val lista = itens(producaoId)
        clientesInvalidos().forEach { clientes ->
            assertFechamentoRejeitado(producaoId, lista, clientes)
        }
    }

    @Test
    fun quantidadeProduzidaInvalidaNoSegundoItemPreservaTodosOsDados() {
        val producaoId = criarProducao()
        val lista = itens(producaoId)
        quantidadesProduzidasInvalidas().forEach { quantidade ->
            assertFechamentoRejeitado(
                producaoId,
                listOf(lista.first().copy(quantidadeSobra = 1.0), lista.last().copy(quantidadeProduzida = quantidade))
            )
        }
    }

    @Test
    fun sobraInvalidaNoSegundoItemPreservaTodosOsDados() {
        val producaoId = criarProducao()
        val lista = itens(producaoId)
        sobrasInvalidas(lista.last()).forEach { sobra ->
            assertFechamentoRejeitado(
                producaoId,
                listOf(lista.first().copy(quantidadeSobra = 1.0), lista.last().copy(quantidadeSobra = sobra))
            )
        }
    }

    @Test
    fun horarioObrigatorioInvalidoNoSegundoItemPreservaTodosOsDados() {
        val producaoId = criarProducao()
        val lista = itens(producaoId)
        horariosInvalidos().forEach { horario ->
            assertFechamentoRejeitado(
                producaoId,
                listOf(
                    lista.first().copy(quantidadeSobra = 1.0),
                    lista.last().copy(acabouAntesDoFim = true, horarioAcabou = horario)
                )
            )
        }
    }

    @Test
    fun correcaoComDadosInvalidosPreservaFechamentoAnteriorEDataDaAlteracao() {
        val producaoId = criarProducao()
        val lista = itens(producaoId)
        producoes.fecharProducao(producaoId, 100, lista.map { it.copy(quantidadeSobra = 1.0) })
        producoes.fecharProducao(producaoId, 110, lista.map { it.copy(quantidadeSobra = 2.0) })
        assertTrue(!producoes.listarHistorico().single().day.alteradoEm.isNullOrBlank())

        clientesInvalidos().forEach { assertFechamentoRejeitado(producaoId, lista, it) }
        val segundo = lista.last()
        val itensInvalidos = quantidadesProduzidasInvalidas().map { segundo.copy(quantidadeProduzida = it) } +
            sobrasInvalidas(segundo).map { segundo.copy(quantidadeSobra = it) } +
            horariosInvalidos().map { segundo.copy(acabouAntesDoFim = true, horarioAcabou = it) }
        itensInvalidos.forEach { segundoInvalido ->
            assertFechamentoRejeitado(
                producaoId, listOf(lista.first().copy(quantidadeSobra = 3.0), segundoInvalido)
            )
        }
    }

    @Test
    fun limitesValidosPermitemFechamentoECorrecao() {
        val producaoId = criarProducao()
        val quantidadeMaxima = ValidarDadosProducaoUseCase.MAXIMA_QUANTIDADE
        val lista = itens(producaoId).map {
            it.copy(quantidadeProduzida = quantidadeMaxima, quantidadeSobra = quantidadeMaxima)
        }
        producoes.fecharProducao(producaoId, ValidarDadosProducaoUseCase.MAXIMO_CLIENTES, lista)
        val fechado = producoes.listarHistorico().single()
        assertTrue(fechado.fechado)
        assertEquals(ValidarDadosProducaoUseCase.MAXIMO_CLIENTES, fechado.day.clientesAtendidos)
        fechado.items.forEach {
            assertEquals(quantidadeMaxima, it.item.quantidadeProduzida, 0.0)
            assertEquals(quantidadeMaxima, it.item.quantidadeSobra, 0.0)
            assertEquals(0.0, it.consumo, 0.0)
        }

        producoes.fecharProducao(producaoId, 1, lista.map { it.copy(quantidadeSobra = 0.0) })
        val corrigido = producoes.listarHistorico().single()
        assertEquals(1, corrigido.day.clientesAtendidos)
        assertTrue(corrigido.items.all { it.item.quantidadeSobra == 0.0 })
    }

    @Test
    fun horariosValidosNosLimitesSaoSalvosSemEspacos() {
        val producaoId = criarProducao()
        val lista = itens(producaoId)
        producoes.fecharProducao(
            producaoId, 100,
            listOf(
                lista.first().copy(acabouAntesDoFim = true, horarioAcabou = " 00:00 "),
                lista.last().copy(acabouAntesDoFim = true, horarioAcabou = " 23:59 ")
            )
        )

        val salvos = itens(producaoId).associateBy { it.id }
        assertEquals("00:00", salvos.getValue(lista.first().id).horarioAcabou)
        assertEquals("23:59", salvos.getValue(lista.last().id).horarioAcabou)
        assertTrue(salvos.values.all { it.acabouAntesDoFim })
    }

    @Test
    fun horarioNaoUsadoEIgnoradoQuandoPreparoNaoAcabouAntesDoFim() {
        val producaoId = criarProducao()
        producoes.fecharProducao(
            producaoId, 100,
            itens(producaoId).map { it.copy(acabouAntesDoFim = false, horarioAcabou = "horario antigo") }
        )

        itens(producaoId).forEach { assertNull(it.horarioAcabou) }
    }

    private fun clientesInvalidos() = listOf(-1, 0, ValidarDadosProducaoUseCase.MAXIMO_CLIENTES + 1)

    private fun quantidadesProduzidasInvalidas() = listOf(
        Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY,
        -1.0, 0.0, ValidarDadosProducaoUseCase.MAXIMA_QUANTIDADE + 1.0
    )

    private fun sobrasInvalidas(item: ItemProducao) = listOf(
        Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY,
        -1.0, item.quantidadeProduzida + 1.0, ValidarDadosProducaoUseCase.MAXIMA_QUANTIDADE + 1.0
    )

    private fun horariosInvalidos(): List<String?> = listOf(null, "", " ", "24:00", "12:60", "9:30", "abc")

    private fun criarProducao(turno: Turno = Turno.ALMOCO): Long = producoes.salvarProducao(
        ProducaoDia(data = "2026-10-04", diaDaSemana = 7, turno = turno),
        linkedMapOf(arrozId to 10.0, feijaoId to 5.0)
    )

    private fun itens(producaoId: Long): List<ItemProducao> =
        producoes.listarHistorico().single { it.day.id == producaoId }.items.map { it.item }

    private fun assertFechamentoRejeitado(producaoId: Long, lista: List<ItemProducao>, clientes: Int = 999) {
        val historicoAntes = database.producaoDao().listarHistorico()
        assertThrows(IllegalArgumentException::class.java) {
            producoes.fecharProducao(producaoId, clientes, lista)
        }
        assertEquals(historicoAntes, database.producaoDao().listarHistorico())
    }
}
