package com.project.semsobra.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.project.semsobra.data.local.room.SemSobraDatabase
import com.project.semsobra.domain.exception.PreparoIndisponivelException
import com.project.semsobra.domain.exception.ProducaoFechadaException
import com.project.semsobra.domain.model.Preparo
import com.project.semsobra.domain.model.ProducaoDia
import com.project.semsobra.domain.previsao.model.Turno
import com.project.semsobra.domain.usecase.ExcluirPreparoUseCase
import com.project.semsobra.domain.usecase.ValidarDadosProducaoUseCase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RepositoriosLocaisTest {
    private lateinit var database: SemSobraDatabase
    private lateinit var preparos: PreparoLocalRepository
    private lateinit var producoes: ProducaoLocalRepository

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            SemSobraDatabase::class.java
        ).allowMainThreadQueries().build()
        preparos = PreparoLocalRepository(database.preparoDao())
        producoes = ProducaoLocalRepository(database)
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun atualizarPreparoInativoPreservaInativacao() {
        val preparoId = preparos.inserir(Preparo("Arroz", "", "kg", 1))
        assertTrue(preparos.inativar(preparoId))

        assertTrue(preparos.atualizar(Preparo(preparoId, "Arroz integral", "", "kg", 1, 1)))

        assertFalse(database.preparoDao().buscarPorId(preparoId)!!.ativo)
        assertTrue(preparos.listarTodos().isEmpty())
        assertEquals("Arroz integral", preparos.buscarPorId(preparoId).get().nome)
    }

    @Test
    fun excluirPreparoUsadoInativaEPreservaHistorico() {
        val preparoId = preparos.inserir(Preparo("Arroz", "", "kg", 1))
        val producaoId = producoes.salvarProducao(
            ProducaoDia(data = "2026-10-03", diaDaSemana = 6),
            mapOf(preparoId to 10.0)
        )

        val resultado = ExcluirPreparoUseCase(preparos).executar(preparoId)

        assertEquals(ExcluirPreparoUseCase.Resultado.INATIVADO_POR_HISTORICO, resultado)
        assertTrue(preparos.listarTodos().isEmpty())
        val historico = producoes.listarHistorico().single()
        assertEquals(producaoId, historico.day.id)
        assertEquals(preparoId, historico.items.single().food.id)
        assertEquals("Arroz", historico.items.single().food.nome)
        assertEquals(10.0, historico.items.single().item.quantidadeProduzida, 0.0)
    }

    @Test
    fun fechamentoComItemInexistentePreservaTodasAsInformacoes() {
        val preparoId = preparos.inserir(Preparo("Arroz", "", "kg", 1))
        val producaoId = producoes.salvarProducao(
            ProducaoDia(data = "2026-10-03", diaDaSemana = 6),
            mapOf(preparoId to 10.0)
        )
        val itemOriginal = producoes.listarHistorico().single().items.single().item

        assertThrows(IllegalArgumentException::class.java) {
            producoes.fecharProducao(
                producaoId,
                100,
                listOf(
                    itemOriginal.copy(quantidadeSobra = 2.0),
                    itemOriginal.copy(id = Long.MAX_VALUE, quantidadeSobra = 1.0)
                )
            )
        }

        val historico = producoes.listarHistorico().single()
        assertFalse(historico.fechado)
        assertEquals(0, historico.day.clientesAtendidos)
        assertEquals(itemOriginal, historico.items.single().item)
    }

    @Test
    fun salvarProducaoFechadaBloqueiaSobrescritaEPreservaTodosOsDados() {
        val preparoId = preparos.inserir(Preparo("Arroz", "", "kg", 1))
        val producao = ProducaoDia(data = "2026-10-03", diaDaSemana = 6)
        val producaoId = producoes.salvarProducao(producao, mapOf(preparoId to 10.0))
        val item = producoes.listarHistorico().single().items.single().item
        producoes.fecharProducao(
            producaoId, 100,
            listOf(item.copy(quantidadeSobra = 2.0, acabouAntesDoFim = true, horarioAcabou = "13:30"))
        )
        producoes.fecharProducao(
            producaoId, 110,
            listOf(item.copy(quantidadeSobra = 1.0, acabouAntesDoFim = true, horarioAcabou = "13:30"))
        )
        val historicoAntes = database.producaoDao().listarHistorico()

        assertThrows(ProducaoFechadaException::class.java) {
            producoes.salvarProducao(producao, mapOf(preparoId to 20.0))
        }

        assertEquals(historicoAntes, database.producaoDao().listarHistorico())
        assertTrue(producoes.listarHistorico().single().fechado)
    }

    @Test
    fun salvarProducaoAbertaPermiteAtualizarQuantidades() {
        val preparoId = preparos.inserir(Preparo("Arroz", "", "kg", 1))
        val producao = ProducaoDia(data = "2026-10-03", diaDaSemana = 6)
        val producaoId = producoes.salvarProducao(producao, mapOf(preparoId to 10.0))

        val atualizadoId = producoes.salvarProducao(producao, mapOf(preparoId to 7.5))

        assertEquals(producaoId, atualizadoId)
        val historico = producoes.listarHistorico().single()
        assertFalse(historico.fechado)
        assertEquals(7.5, historico.items.single().item.quantidadeProduzida, 0.0)
    }

    @Test
    fun fechamentoDeUmTurnoNaoBloqueiaProducaoDeOutroTurno() {
        val preparoId = preparos.inserir(Preparo("Arroz", "", "kg", 1))
        val almoco = ProducaoDia(data = "2026-10-03", diaDaSemana = 6, turno = Turno.ALMOCO)
        val almocoId = producoes.salvarProducao(almoco, mapOf(preparoId to 10.0))
        val item = producoes.listarHistorico().single().items.single().item
        producoes.fecharProducao(almocoId, 100, listOf(item.copy(quantidadeSobra = 2.0)))
        val almocoFechado = database.producaoDao().listarHistorico()

        val jantarId = producoes.salvarProducao(
            almoco.copy(turno = Turno.JANTAR), mapOf(preparoId to 5.0)
        )

        val historico = producoes.listarHistorico()
        assertEquals(2, historico.size)
        assertEquals(
            almocoFechado,
            database.producaoDao().listarHistorico().filter { it.producaoId == almocoId }
        )
        val jantar = historico.single { it.day.id == jantarId }
        assertEquals(Turno.JANTAR, jantar.day.turno)
        assertFalse(jantar.fechado)
    }

    @Test
    fun producaoFechadaContinuaPermitindoCorrecaoPeloFechamento() {
        val preparoId = preparos.inserir(Preparo("Arroz", "", "kg", 1))
        val producaoId = producoes.salvarProducao(
            ProducaoDia(data = "2026-10-03", diaDaSemana = 6), mapOf(preparoId to 10.0)
        )
        val item = producoes.listarHistorico().single().items.single().item
        producoes.fecharProducao(producaoId, 100, listOf(item.copy(quantidadeSobra = 2.0)))

        producoes.fecharProducao(producaoId, 110, listOf(item.copy(quantidadeSobra = 1.0)))

        val historico = producoes.listarHistorico().single()
        assertTrue(historico.fechado)
        assertEquals(110, historico.day.clientesAtendidos)
        assertEquals(1.0, historico.items.single().item.quantidadeSobra, 0.0)
        assertTrue(!historico.day.alteradoEm.isNullOrBlank())
    }

    @Test
    fun preparoInativoNaoPodeSerUsadoEmNovaProducao() {
        val preparoId = preparos.inserir(Preparo("Arroz", "", "kg", 1))
        assertTrue(preparos.inativar(preparoId))
        val producao = ProducaoDia(data = "2026-10-03", diaDaSemana = 6)

        assertThrows(PreparoIndisponivelException::class.java) {
            producoes.salvarProducao(producao, mapOf(preparoId to 10.0))
        }

        assertNull(database.producaoDao().buscarId(producao.data, producao.turno.name))
        assertTrue(producoes.listarHistorico().isEmpty())
    }

    @Test
    fun preparoInativoBloqueiaTodaProducaoMesmoComOutrosPreparosAtivos() {
        val ativoId = preparos.inserir(Preparo("Arroz", "", "kg", 1))
        val inativoId = preparos.inserir(Preparo("Feijão", "", "kg", 1))
        assertTrue(preparos.inativar(inativoId))
        val producao = ProducaoDia(data = "2026-10-03", diaDaSemana = 6)

        assertThrows(PreparoIndisponivelException::class.java) {
            producoes.salvarProducao(producao, linkedMapOf(ativoId to 10.0, inativoId to 5.0))
        }

        assertNull(database.producaoDao().buscarId(producao.data, producao.turno.name))
        assertTrue(database.producaoDao().listarHistorico().isEmpty())
    }

    @Test
    fun tentativaDeSalvarComPreparoInativoPreservaProducaoAbertaExistente() {
        val preparoId = preparos.inserir(Preparo("Arroz", "", "kg", 1))
        val outroPreparoId = preparos.inserir(Preparo("Feijão", "", "kg", 1))
        val producao = ProducaoDia(data = "2026-10-03", diaDaSemana = 6)
        val producaoId = producoes.salvarProducao(producao, mapOf(preparoId to 10.0))
        assertTrue(preparos.inativar(preparoId))
        val historicoAntes = database.producaoDao().listarHistorico()

        assertThrows(PreparoIndisponivelException::class.java) {
            producoes.salvarProducao(
                producao.copy(restauranteAberto = false),
                linkedMapOf(outroPreparoId to 5.0, preparoId to 20.0)
            )
        }

        assertEquals(producaoId, database.producaoDao().buscarId(producao.data, producao.turno.name))
        assertEquals(historicoAntes, database.producaoDao().listarHistorico())
        assertFalse(producoes.listarHistorico().single().fechado)
    }

    @Test
    fun preparoInativoNoHistoricoPermiteFechamentoECorrecao() {
        val preparoId = preparos.inserir(Preparo("Arroz", "", "kg", 1))
        val producaoId = producoes.salvarProducao(
            ProducaoDia(data = "2026-10-03", diaDaSemana = 6), mapOf(preparoId to 10.0)
        )
        val item = producoes.listarHistorico().single().items.single().item
        assertTrue(preparos.inativar(preparoId))

        producoes.fecharProducao(producaoId, 100, listOf(item.copy(quantidadeSobra = 2.0)))
        val fechado = producoes.listarHistorico().single()
        assertTrue(fechado.fechado)
        assertEquals(100, fechado.day.clientesAtendidos)
        assertEquals(2.0, fechado.items.single().item.quantidadeSobra, 0.0)

        producoes.fecharProducao(producaoId, 110, listOf(item.copy(quantidadeSobra = 1.0)))

        val corrigido = producoes.listarHistorico().single()
        assertTrue(corrigido.fechado)
        assertEquals(110, corrigido.day.clientesAtendidos)
        assertEquals(1.0, corrigido.items.single().item.quantidadeSobra, 0.0)
        assertTrue(!corrigido.day.alteradoEm.isNullOrBlank())
        assertFalse(database.preparoDao().buscarPorId(preparoId)!!.ativo)
        assertTrue(preparos.listarTodos().isEmpty())
    }

    @Test
    fun preparoInexistenteBloqueiaProducaoSemCriarRegistroParcial() {
        val producao = ProducaoDia(data = "2026-10-03", diaDaSemana = 6)

        assertThrows(PreparoIndisponivelException::class.java) {
            producoes.salvarProducao(producao, mapOf(Long.MAX_VALUE to 10.0))
        }

        assertNull(database.producaoDao().buscarId(producao.data, producao.turno.name))
        assertTrue(producoes.listarHistorico().isEmpty())
    }

    @Test
    fun quantidadeInvalidaNaoCriaProducaoParcial() {
        val preparoId = preparos.inserir(Preparo("Arroz", "", "kg", 1))
        val outroPreparoId = preparos.inserir(Preparo("Feijão", "", "kg", 1))
        val producao = ProducaoDia(data = "2026-10-03", diaDaSemana = 6)

        quantidadesInvalidas().forEach { quantidade ->
            assertThrows(IllegalArgumentException::class.java) {
                producoes.salvarProducao(
                    producao, linkedMapOf(preparoId to 10.0, outroPreparoId to quantidade)
                )
            }
            assertNull(database.producaoDao().buscarId(producao.data, producao.turno.name))
            assertTrue(database.producaoDao().listarHistorico().isEmpty())
        }
    }

    @Test
    fun quantidadeInvalidaPreservaProducaoAbertaExistente() {
        val preparoId = preparos.inserir(Preparo("Arroz", "", "kg", 1))
        val outroPreparoId = preparos.inserir(Preparo("Feijão", "", "kg", 1))
        val producao = ProducaoDia(data = "2026-10-03", diaDaSemana = 6)
        val producaoId = producoes.salvarProducao(producao, mapOf(preparoId to 10.0))
        val historicoAntes = database.producaoDao().listarHistorico()

        quantidadesInvalidas().forEach { quantidade ->
            assertThrows(IllegalArgumentException::class.java) {
                producoes.salvarProducao(
                    producao.copy(restauranteAberto = false),
                    linkedMapOf(preparoId to 20.0, outroPreparoId to quantidade)
                )
            }
            assertEquals(producaoId, database.producaoDao().buscarId(producao.data, producao.turno.name))
            assertEquals(historicoAntes, database.producaoDao().listarHistorico())
        }
    }

    @Test
    fun quantidadeNoLimiteMaximoPodeSerSalva() {
        val preparoId = preparos.inserir(Preparo("Arroz", "", "kg", 1))
        producoes.salvarProducao(
            ProducaoDia(data = "2026-10-03", diaDaSemana = 6),
            mapOf(preparoId to ValidarDadosProducaoUseCase.MAXIMA_QUANTIDADE)
        )

        assertEquals(
            ValidarDadosProducaoUseCase.MAXIMA_QUANTIDADE,
            producoes.listarHistorico().single().items.single().item.quantidadeProduzida,
            0.0
        )
    }

    private fun quantidadesInvalidas() = listOf(
        Double.NaN,
        Double.POSITIVE_INFINITY,
        Double.NEGATIVE_INFINITY,
        -1.0,
        0.0,
        ValidarDadosProducaoUseCase.MAXIMA_QUANTIDADE + 1.0
    )
}
