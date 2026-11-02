package com.project.semsobra.data.backup

import android.content.Context
import android.database.sqlite.SQLiteException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.project.semsobra.data.local.room.ItemProducaoEntity
import com.project.semsobra.data.local.room.PreparoEntity
import com.project.semsobra.data.local.room.ProducaoEntity
import com.project.semsobra.data.local.room.SemSobraDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SemSobraBackupRepositoryTest {
    private lateinit var database: SemSobraDatabase
    private lateinit var repository: SemSobraBackupRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            SemSobraDatabase::class.java
        ).allowMainThreadQueries().build()
        repository = SemSobraBackupRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun exportacaoEImportacaoPreservamTodosOsRegistrosEIds() = runBlocking {
        val expected = backup(1)
        repository.restaurar(BackupJsonCodec.encode(expected))

        val exported = repository.exportar()
        val replacement = backup(100)
        repository.restaurar(BackupJsonCodec.encode(replacement))

        assertEquals(replacement, BackupJsonCodec.decode(repository.exportar()).backup)
        assertEquals(expected.preparos.size, BackupJsonCodec.decode(exported).backup.preparos.size)
    }

    @Test
    fun falhaAoInserirItemDesfazTodaARestauracao() = runBlocking {
        val original = backup(1)
        repository.restaurar(BackupJsonCodec.encode(original))
        database.openHelper.writableDatabase.execSQL(
            "CREATE TRIGGER falhar_importacao_backup BEFORE INSERT ON itens_producao " +
                "BEGIN SELECT RAISE(ABORT, 'Falha simulada na importação'); END"
        )

        assertThrows(SQLiteException::class.java) {
            runBlocking { repository.restaurar(BackupJsonCodec.encode(backup(100))) }
        }

        assertEquals(original, BackupJsonCodec.decode(repository.exportar()).backup)
    }

    @Test
    fun arquivoInvalidoNaoApagaDadosAtuais() = runBlocking {
        val original = backup(1)
        repository.restaurar(BackupJsonCodec.encode(original))

        assertThrows(BackupFormatException::class.java) {
            runBlocking { repository.restaurar("{}") }
        }

        assertEquals(original, BackupJsonCodec.decode(repository.exportar()).backup)
    }

    private fun backup(id: Long) = SemSobraBackup(
        preparos = listOf(
            PreparoEntity(id, "Arroz $id", "Receita", "kg", 0, 127, true),
            PreparoEntity(id + 1, "Feijão $id", "", "kg", 1, 1, false)
        ),
        producoes = listOf(
            ProducaoEntity(
                id + 2,
                "2026-10-30",
                5,
                100,
                "ALMOCO",
                restauranteAberto = true,
                fechada = true
            )
        ),
        itens = listOf(
            ItemProducaoEntity(id + 3, id + 2, id, 10.0, 1.25, true, "13:40"),
            ItemProducaoEntity(id + 4, id + 2, id + 1, 5.0, 0.0, false, null)
        )
    )
}
