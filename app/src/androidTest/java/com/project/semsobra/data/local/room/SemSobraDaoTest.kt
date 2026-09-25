package com.project.semsobra.data.local.room

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SemSobraDaoTest {
    private lateinit var database: SemSobraDatabase

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            SemSobraDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun preparoDao_emitePreparosAtivosPeloFlow() = runBlocking {
        val dao = database.preparoDao()
        val id = dao.inserir(
            PreparoEntity(nome = "Arroz", unidadeMedida = "kg", diaDaSemana = 1)
        )

        assertEquals(listOf("Arroz"), dao.observarAtivos().first().map { it.nome })
        assertTrue(dao.inativar(id) > 0)
        assertTrue(dao.observarAtivos().first().isEmpty())
    }

    @Test
    fun preparoDao_aplicaUnicidadePorNomeEDia() {
        val dao = database.preparoDao()
        dao.inserir(PreparoEntity(nome = "Arroz", unidadeMedida = "kg", diaDaSemana = 1))

        assertTrue(dao.existeNome("arroz", 1, null))
        assertFalse(dao.existeNome("arroz", 2, null))
    }

    @Test
    fun producaoDao_emiteHistoricoRelacionadoPeloFlow() = runBlocking {
        val preparoId = database.preparoDao().inserir(
            PreparoEntity(nome = "Feijão", unidadeMedida = "kg", diaDaSemana = 2)
        )
        val producaoId = database.producaoDao().inserirProducao(
            ProducaoEntity(data = "2026-09-24", diaDaSemana = 4, turno = "ALMOCO")
        )
        database.producaoDao().inserirItem(
            ItemProducaoEntity(
                producaoId = producaoId,
                preparoId = preparoId,
                quantidadeProduzida = 10.0
            )
        )

        val rows = database.producaoDao().observarHistorico().first()
        assertEquals(1, rows.size)
        assertEquals("Feijão", rows.single().nome)
        assertEquals(10.0, rows.single().quantidadeProduzida, 0.0)
    }
}
