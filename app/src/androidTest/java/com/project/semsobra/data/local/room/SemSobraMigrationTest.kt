package com.project.semsobra.data.local.room

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.json.JSONObject

@RunWith(AndroidJUnit4::class)
class SemSobraMigrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val databaseName = "migration-test.db"

    @After
    fun cleanup() {
        context.deleteDatabase(databaseName)
    }

    @Test
    fun migration1To7_preservaPreparosEPermiteNovaProducao() {
        val preparosEsperados = (0..7).map { dia ->
            PreparoEntity(
                id = (dia + 1) * 10L,
                nome = "Preparo $dia",
                descricao = "Receita do dia $dia",
                unidadeMedida = if (dia % 2 == 0) "kg" else "un",
                diaDaSemana = dia,
                diasSemanaMask = if (dia == 0) 127 else 1 shl (dia - 1),
                ativo = true
            )
        }
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(databaseName)
                .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                    override fun onConfigure(db: SupportSQLiteDatabase) {
                        db.setForeignKeyConstraintsEnabled(true)
                    }

                    override fun onCreate(db: SupportSQLiteDatabase) {
                        criarSchemaVersao1(db)
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
            val legacy = helper.writableDatabase
            assertEquals(1, legacy.version)
            preparosEsperados.forEach { preparo ->
                legacy.execSQL(
                    "INSERT INTO preparos " +
                        "(id, nome, descricao, unidade_medida, dia_da_semana) " +
                        "VALUES (?, ?, ?, ?, ?)",
                    arrayOf<Any>(
                        preparo.id, preparo.nome, preparo.descricao,
                        preparo.unidadeMedida, preparo.diaDaSemana
                    )
                )
            }
        } finally {
            helper.close()
        }

        val migrated = Room.databaseBuilder(context, SemSobraDatabase::class.java, databaseName)
            .addMigrations(
                SemSobraDatabase.MIGRATION_1_2,
                SemSobraDatabase.MIGRATION_2_3,
                SemSobraDatabase.MIGRATION_3_4,
                SemSobraDatabase.MIGRATION_4_5,
                SemSobraDatabase.MIGRATION_5_6,
                SemSobraDatabase.MIGRATION_6_7
            )
            .allowMainThreadQueries()
            .build()

        val historicoEsperado: List<HistoricoRow>
        try {
            assertEquals(7, migrated.openHelper.writableDatabase.version)
            assertEquals(preparosEsperados, migrated.preparoDao().listarAtivos())
            preparosEsperados.forEach { preparo ->
                assertEquals(preparo, migrated.preparoDao().buscarPorId(preparo.id))
            }
            assertTrue(migrated.producaoDao().listarHistorico().isEmpty())

            val producaoId = migrated.producaoDao().inserirProducao(
                ProducaoEntity(data = "2026-10-05", diaDaSemana = 1, turno = "ALMOCO")
            )
            val item = ItemProducaoEntity(
                producaoId = producaoId, preparoId = preparosEsperados.first().id,
                quantidadeProduzida = 2.5
            )
            val itemId = migrated.producaoDao().inserirItem(item)
            assertEquals(listOf(item.copy(id = itemId)), migrated.producaoDao().listarItens(producaoId))
            historicoEsperado = listOf(
                HistoricoRow(
                    producaoId = producaoId, data = "2026-10-05", producaoDiaDaSemana = 1,
                    clientesAtendidos = 0, turno = "ALMOCO", restauranteAberto = true,
                    fechada = false, alteradoEm = null, itemId = itemId, preparoId = 10,
                    quantidadeProduzida = 2.5, quantidadeSobra = 0.0,
                    acabouAntesDoFim = false, horarioAcabou = null, nome = "Preparo 0",
                    descricao = "Receita do dia 0", unidadeMedida = "kg", preparoDiaDaSemana = 0,
                    diasSemanaMask = 127
                )
            )
            assertEquals(historicoEsperado, migrated.producaoDao().listarHistorico())
            migrated.openHelper.writableDatabase.query("PRAGMA foreign_key_check").use {
                assertFalse(it.moveToFirst())
            }
        } finally {
            migrated.close()
        }

        val reopened = Room.databaseBuilder(context, SemSobraDatabase::class.java, databaseName)
            .allowMainThreadQueries()
            .build()
        try {
            assertEquals(7, reopened.openHelper.writableDatabase.version)
            assertEquals(preparosEsperados, reopened.preparoDao().listarAtivos())
            assertEquals(historicoEsperado, reopened.producaoDao().listarHistorico())
        } finally {
            reopened.close()
        }
    }

    @Test
    fun migration2To7_preservaPreparosProducoesEItens() {
        testarMigracaoComHistorico(2)
    }

    @Test
    fun migration3To7_preservaPreparosInativosProducoesEItens() {
        testarMigracaoComHistorico(3)
    }

    @Test
    fun migration4To7_preservaPreparosProducoesEItens() {
        testarMigracaoComHistorico(4)
    }

    @Test
    fun migration5To7_preservaPreparosInativosProducoesEItens() {
        testarMigracaoComHistorico(5)
    }

    @Test
    fun migration6To7_preservaHistoricoEDataDeCorrecao() {
        testarMigracaoComHistorico(6)
    }

    private fun testarMigracaoComHistorico(versaoInicial: Int) {
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(databaseName)
                .callback(object : SupportSQLiteOpenHelper.Callback(versaoInicial) {
                    override fun onConfigure(db: SupportSQLiteDatabase) {
                        db.setForeignKeyConstraintsEnabled(true)
                    }

                    override fun onCreate(db: SupportSQLiteDatabase) {
                        when (versaoInicial) {
                            2, 3 -> criarSchemaVersao2Ou3(db, versaoInicial)
                            4 -> criarSchemaVersao4(db)
                            5, 6 -> criarSchemaRoomExportado(db, versaoInicial)
                            else -> error("Versão sem fixture de histórico: $versaoInicial")
                        }
                    }

                    override fun onUpgrade(
                        db: SupportSQLiteDatabase,
                        oldVersion: Int,
                        newVersion: Int
                    ) = Unit
                })
                .build()
        )
        val alteradoEmEsperado = if (versaoInicial == 6) "2026-09-24 14:15:00" else null
        try {
            helper.writableDatabase.apply {
                assertEquals(versaoInicial, version)
                if (versaoInicial == 2) {
                    execSQL(
                        "INSERT INTO preparos " +
                            "(id, nome, descricao, unidade_medida, dia_da_semana) " +
                            "VALUES (1, 'Arroz', 'Arroz branco', 'kg', 1), " +
                            "(2, 'Feijão', 'Feijão preto', 'kg', 0)"
                    )
                } else {
                    execSQL(
                        "INSERT INTO preparos " +
                            "(id, nome, descricao, unidade_medida, dia_da_semana, ativo) " +
                            "VALUES (1, 'Arroz', 'Arroz branco', 'kg', 1, 1), " +
                            "(2, 'Feijão', 'Feijão preto', 'kg', 0, 0)"
                    )
                }
                execSQL(
                    "INSERT INTO producoes " +
                        "(id, data, dia_da_semana, clientes_atendidos, turno, " +
                        "restaurante_aberto, fechada) " +
                        "VALUES (1, '2026-09-24', 4, 100, 'ALMOCO', 1, 1), " +
                        "(2, '2026-09-24', 4, 0, 'JANTAR', 0, 0)"
                )
                if (alteradoEmEsperado != null) {
                    execSQL(
                        "UPDATE producoes SET alterado_em = ? WHERE id = 1",
                        arrayOf<Any>(alteradoEmEsperado)
                    )
                }
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
                SemSobraDatabase.MIGRATION_2_3,
                SemSobraDatabase.MIGRATION_3_4,
                SemSobraDatabase.MIGRATION_4_5,
                SemSobraDatabase.MIGRATION_5_6,
                SemSobraDatabase.MIGRATION_6_7
            )
            .allowMainThreadQueries()
            .build()

        val historicoFinalEsperado: List<HistoricoRow>
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
                    diaDaSemana = 0, diasSemanaMask = 127, ativo = versaoInicial == 2
                ),
                migrated.preparoDao().buscarPorId(2)
            )
            assertEquals(
                if (versaoInicial == 2) listOf(1L, 2L) else listOf(1L),
                migrated.preparoDao().listarAtivos().map { it.id }
            )

            val esperado = listOf(
                HistoricoRow(
                    producaoId = 1, data = "2026-09-24", producaoDiaDaSemana = 4,
                    clientesAtendidos = 100, turno = "ALMOCO", restauranteAberto = true,
                    fechada = true, alteradoEm = alteradoEmEsperado, itemId = 1, preparoId = 1,
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
            historicoFinalEsperado = esperado + esperado.first().copy(
                producaoId = 2, clientesAtendidos = 0, turno = "JANTAR",
                restauranteAberto = false, fechada = false, alteradoEm = null, itemId = novoItemId,
                quantidadeProduzida = 3.0, quantidadeSobra = 0.0,
                acabouAntesDoFim = false, horarioAcabou = null
            )
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

        val reopened = Room.databaseBuilder(context, SemSobraDatabase::class.java, databaseName)
            .allowMainThreadQueries()
            .build()
        try {
            assertEquals(7, reopened.openHelper.writableDatabase.version)
            assertEquals(versaoInicial == 2, reopened.preparoDao().buscarPorId(2)?.ativo)
            assertEquals(
                historicoFinalEsperado.associateBy { it.itemId },
                reopened.producaoDao().listarHistorico().associateBy { it.itemId }
            )
        } finally {
            reopened.close()
        }
    }

    private fun criarSchemaRoomExportado(db: SupportSQLiteDatabase, versao: Int) {
        val schemaPath = "${SemSobraDatabase::class.java.canonicalName}/$versao.json"
        val schema = InstrumentationRegistry.getInstrumentation().context.assets
            .open(schemaPath).bufferedReader().use {
                JSONObject(it.readText()).getJSONObject("database")
            }
        assertEquals(versao, schema.getInt("version"))
        val entidades = schema.getJSONArray("entities")
        for (indice in 0 until entidades.length()) {
            val entidade = entidades.getJSONObject(indice)
            val tabela = entidade.getString("tableName")
            db.execSQL(entidade.getString("createSql").replace("\${TABLE_NAME}", tabela))
            val indices = entidade.getJSONArray("indices")
            for (indiceTabela in 0 until indices.length()) {
                db.execSQL(
                    indices.getJSONObject(indiceTabela).getString("createSql")
                        .replace("\${TABLE_NAME}", tabela)
                )
            }
        }
        val setupQueries = schema.getJSONArray("setupQueries")
        for (indice in 0 until setupQueries.length()) {
            db.execSQL(setupQueries.getString(indice))
        }
    }

    private fun criarSchemaVersao1(db: SupportSQLiteDatabase) {
        // Estrutura original do SQLiteOpenHelper no commit 6af5220.
        db.execSQL(
            """CREATE TABLE preparos (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nome TEXT NOT NULL COLLATE NOCASE UNIQUE,
                descricao TEXT NOT NULL DEFAULT '',
                unidade_medida TEXT NOT NULL,
                dia_da_semana INTEGER NOT NULL CHECK (dia_da_semana BETWEEN 0 AND 7))"""
        )
    }

    private fun criarSchemaVersao2Ou3(db: SupportSQLiteDatabase, versao: Int) {
        // Estruturas originais nos commits da56106 (v2) e a98a08c (v3).
        val colunaAtivo = if (versao == 3) {
            ", ativo INTEGER NOT NULL DEFAULT 1 CHECK (ativo IN (0, 1))"
        } else {
            ""
        }
        db.execSQL(
            """CREATE TABLE preparos (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nome TEXT NOT NULL COLLATE NOCASE UNIQUE,
                descricao TEXT NOT NULL DEFAULT '',
                unidade_medida TEXT NOT NULL,
                dia_da_semana INTEGER NOT NULL CHECK (dia_da_semana BETWEEN 0 AND 7)
                $colunaAtivo)"""
        )
        db.execSQL(
            """CREATE TABLE producoes (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                data TEXT NOT NULL,
                dia_da_semana INTEGER NOT NULL CHECK (dia_da_semana BETWEEN 1 AND 7),
                clientes_atendidos INTEGER NOT NULL DEFAULT 0 CHECK (clientes_atendidos >= 0),
                turno TEXT NOT NULL,
                restaurante_aberto INTEGER NOT NULL DEFAULT 1 CHECK (restaurante_aberto IN (0, 1)),
                fechada INTEGER NOT NULL DEFAULT 0 CHECK (fechada IN (0, 1)),
                UNIQUE (data, turno))"""
        )
        db.execSQL(
            """CREATE TABLE itens_producao (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                producao_id INTEGER NOT NULL,
                preparo_id INTEGER NOT NULL,
                quantidade_produzida REAL NOT NULL CHECK (quantidade_produzida >= 0),
                quantidade_sobra REAL NOT NULL DEFAULT 0
                    CHECK (quantidade_sobra >= 0 AND quantidade_sobra <= quantidade_produzida),
                acabou_antes_do_fim INTEGER NOT NULL DEFAULT 0 CHECK (acabou_antes_do_fim IN (0, 1)),
                horario_acabou TEXT,
                FOREIGN KEY (producao_id) REFERENCES producoes(id) ON DELETE CASCADE,
                FOREIGN KEY (preparo_id) REFERENCES preparos(id) ON DELETE RESTRICT,
                UNIQUE (producao_id, preparo_id))"""
        )
        db.execSQL("CREATE INDEX indice_itens_producao_preparo ON itens_producao(preparo_id)")
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
