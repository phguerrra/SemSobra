package com.project.semsobra.data.local.room

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SemSobraMigrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val databaseName = "migration-test.db"

    @After
    fun cleanup() {
        context.deleteDatabase(databaseName)
    }

    @Test
    fun migration4To7_preservaPreparosProducoesEItens() {
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(databaseName)
                .callback(object : SupportSQLiteOpenHelper.Callback(4) {
                    override fun onConfigure(db: SupportSQLiteDatabase) {
                        db.setForeignKeyConstraintsEnabled(true)
                    }

                    override fun onCreate(db: SupportSQLiteDatabase) {
                        criarSchemaVersao4(db)
                    }

                    override fun onUpgrade(
                        db: SupportSQLiteDatabase,
                        oldVersion: Int,
                        newVersion: Int
                    ) = Unit
                })
                .build()
        )
        try {
            helper.writableDatabase.apply {
                execSQL(
                    "INSERT INTO preparos " +
                        "(id, nome, descricao, unidade_medida, dia_da_semana, ativo) " +
                        "VALUES (1, 'Arroz', 'Arroz branco', 'kg', 1, 1), " +
                        "(2, 'Feijão', 'Feijão preto', 'kg', 0, 0)"
                )
                execSQL(
                    "INSERT INTO producoes " +
                        "(id, data, dia_da_semana, clientes_atendidos, turno, " +
                        "restaurante_aberto, fechada) " +
                        "VALUES (1, '2026-09-24', 4, 100, 'ALMOCO', 1, 1), " +
                        "(2, '2026-09-24', 4, 0, 'JANTAR', 0, 0)"
                )
                execSQL(
                    "INSERT INTO itens_producao " +
                        "(id, producao_id, preparo_id, quantidade_produzida, " +
                        "quantidade_sobra, acabou_antes_do_fim, horario_acabou) " +
                        "VALUES (1, 1, 1, 20.125, 2.25, 1, '13:30'), " +
                        "(2, 2, 2, 5.5, 0.0, 0, NULL)"
                )
            }
        } finally {
            helper.close()
        }

        val migrated = Room.databaseBuilder(context, SemSobraDatabase::class.java, databaseName)
            .addMigrations(
                SemSobraDatabase.MIGRATION_4_5,
                SemSobraDatabase.MIGRATION_5_6,
                SemSobraDatabase.MIGRATION_6_7
            )
            .allowMainThreadQueries()
            .build()

        try {
            assertEquals(7, migrated.openHelper.writableDatabase.version)
            assertEquals(
                PreparoEntity(
                    id = 1, nome = "Arroz", descricao = "Arroz branco", unidadeMedida = "kg",
                    diaDaSemana = 1, diasSemanaMask = 1, ativo = true
                ),
                migrated.preparoDao().buscarPorId(1)
            )
            assertEquals(
                PreparoEntity(
                    id = 2, nome = "Feijão", descricao = "Feijão preto", unidadeMedida = "kg",
                    diaDaSemana = 0, diasSemanaMask = 127, ativo = false
                ),
                migrated.preparoDao().buscarPorId(2)
            )
            assertEquals(listOf(1L), migrated.preparoDao().listarAtivos().map { it.id })

            val esperado = listOf(
                HistoricoRow(
                    producaoId = 1, data = "2026-09-24", producaoDiaDaSemana = 4,
                    clientesAtendidos = 100, turno = "ALMOCO", restauranteAberto = true,
                    fechada = true, alteradoEm = null, itemId = 1, preparoId = 1,
                    quantidadeProduzida = 20.125, quantidadeSobra = 2.25,
                    acabouAntesDoFim = true, horarioAcabou = "13:30", nome = "Arroz",
                    descricao = "Arroz branco", unidadeMedida = "kg", preparoDiaDaSemana = 1,
                    diasSemanaMask = 1
                ),
                HistoricoRow(
                    producaoId = 2, data = "2026-09-24", producaoDiaDaSemana = 4,
                    clientesAtendidos = 0, turno = "JANTAR", restauranteAberto = false,
                    fechada = false, alteradoEm = null, itemId = 2, preparoId = 2,
                    quantidadeProduzida = 5.5, quantidadeSobra = 0.0,
                    acabouAntesDoFim = false, horarioAcabou = null, nome = "Feijão",
                    descricao = "Feijão preto", unidadeMedida = "kg", preparoDiaDaSemana = 0,
                    diasSemanaMask = 127
                )
            )
            assertEquals(
                esperado.associateBy { it.itemId },
                migrated.producaoDao().listarHistorico().associateBy { it.itemId }
            )
            migrated.openHelper.writableDatabase.query("PRAGMA foreign_key_check").use {
                assertFalse(it.moveToFirst())
            }
            val novoItemId = migrated.producaoDao().inserirItem(
                ItemProducaoEntity(producaoId = 2, preparoId = 1, quantidadeProduzida = 3.0)
            )
            assertTrue(novoItemId > 2)
            assertEquals(2, migrated.producaoDao().listarItens(2).size)
            assertThrows(SQLiteConstraintException::class.java) {
                migrated.producaoDao().inserirItem(
                    ItemProducaoEntity(producaoId = 999, preparoId = 1, quantidadeProduzida = 3.0)
                )
            }
            assertThrows(SQLiteConstraintException::class.java) {
                migrated.producaoDao().inserirItem(
                    ItemProducaoEntity(producaoId = 2, preparoId = 999, quantidadeProduzida = 3.0)
                )
            }
        } finally {
            migrated.close()
        }
    }

    private fun criarSchemaVersao4(db: SupportSQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE preparos (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nome TEXT NOT NULL COLLATE NOCASE,
                descricao TEXT NOT NULL DEFAULT '',
                unidade_medida TEXT NOT NULL,
                dia_da_semana INTEGER NOT NULL,
                ativo INTEGER NOT NULL DEFAULT 1,
                UNIQUE (nome, dia_da_semana))"""
        )
        db.execSQL(
            """CREATE TABLE producoes (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                data TEXT NOT NULL,
                dia_da_semana INTEGER NOT NULL,
                clientes_atendidos INTEGER NOT NULL DEFAULT 0,
                turno TEXT NOT NULL,
                restaurante_aberto INTEGER NOT NULL DEFAULT 1,
                fechada INTEGER NOT NULL DEFAULT 0,
                UNIQUE (data, turno))"""
        )
        db.execSQL(
            """CREATE TABLE itens_producao (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                producao_id INTEGER NOT NULL,
                preparo_id INTEGER NOT NULL,
                quantidade_produzida REAL NOT NULL,
                quantidade_sobra REAL NOT NULL DEFAULT 0,
                acabou_antes_do_fim INTEGER NOT NULL DEFAULT 0,
                horario_acabou TEXT,
                FOREIGN KEY (producao_id) REFERENCES producoes(id) ON DELETE CASCADE,
                FOREIGN KEY (preparo_id) REFERENCES preparos(id) ON DELETE RESTRICT,
                UNIQUE (producao_id, preparo_id))"""
        )
        db.execSQL("CREATE INDEX indice_itens_producao_preparo ON itens_producao(preparo_id)")
    }
}
