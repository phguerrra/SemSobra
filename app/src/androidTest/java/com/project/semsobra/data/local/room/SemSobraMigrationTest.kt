package com.project.semsobra.data.local.room

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
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
    fun migration4To5_preservaPreparosProducoesEItens() {
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(databaseName)
                .callback(object : SupportSQLiteOpenHelper.Callback(4) {
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
        helper.writableDatabase.apply {
            execSQL(
                "INSERT INTO preparos " +
                    "(id, nome, descricao, unidade_medida, dia_da_semana, ativo) " +
                    "VALUES (1, 'Arroz', '', 'kg', 1, 1)"
            )
            execSQL(
                "INSERT INTO producoes " +
                    "(id, data, dia_da_semana, clientes_atendidos, turno, " +
                    "restaurante_aberto, fechada) " +
                    "VALUES (1, '2026-09-24', 4, 100, 'ALMOCO', 1, 1)"
            )
            execSQL(
                "INSERT INTO itens_producao " +
                    "(id, producao_id, preparo_id, quantidade_produzida, " +
                    "quantidade_sobra, acabou_antes_do_fim, horario_acabou) " +
                    "VALUES (1, 1, 1, 20.0, 2.0, 0, NULL)"
            )
        }
        helper.close()

        val migrated = Room.databaseBuilder(context, SemSobraDatabase::class.java, databaseName)
            .addMigrations(SemSobraDatabase.MIGRATION_4_5)
            .allowMainThreadQueries()
            .build()

        assertEquals("Arroz", migrated.preparoDao().buscarPorId(1)?.nome)
        assertEquals(1, migrated.producaoDao().listarHistorico().size)
        assertEquals(2.0, migrated.producaoDao().listarHistorico().single().quantidadeSobra, 0.0)
        migrated.close()
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
