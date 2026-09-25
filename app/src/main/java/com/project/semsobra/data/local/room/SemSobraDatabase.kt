package com.project.semsobra.data.local.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [PreparoEntity::class, ProducaoEntity::class, ItemProducaoEntity::class],
    version = 5,
    exportSchema = true
)
abstract class SemSobraDatabase : RoomDatabase() {
    abstract fun preparoDao(): PreparoDao
    abstract fun producaoDao(): ProducaoDao

    companion object {
        private const val DATABASE_NAME = "semsobra.db"

        @Volatile
        private var instance: SemSobraDatabase? = null

        fun getInstance(context: Context): SemSobraDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                SemSobraDatabase::class.java,
                DATABASE_NAME
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .build()
                .also { instance = it }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """CREATE TABLE producoes (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        data TEXT NOT NULL,
                        dia_da_semana INTEGER NOT NULL,
                        clientes_atendidos INTEGER NOT NULL DEFAULT 0,
                        turno TEXT NOT NULL,
                        restaurante_aberto INTEGER NOT NULL DEFAULT 1,
                        fechada INTEGER NOT NULL DEFAULT 0,
                        UNIQUE (data, turno))"""
                )
                database.execSQL(
                    """CREATE TABLE itens_producao (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
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
                database.execSQL(
                    "CREATE INDEX indice_itens_producao_preparo " +
                        "ON itens_producao (preparo_id)"
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE preparos ADD COLUMN ativo INTEGER NOT NULL DEFAULT 1"
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) = Unit
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("PRAGMA defer_foreign_keys = ON")
                criarTabelasRoom(database)
                copiarDados(database)
                database.execSQL("DROP TABLE itens_producao")
                database.execSQL("DROP TABLE producoes")
                database.execSQL("DROP TABLE preparos")
                database.execSQL("ALTER TABLE preparos_room_new RENAME TO preparos")
                database.execSQL("ALTER TABLE producoes_room_new RENAME TO producoes")
                database.execSQL("ALTER TABLE itens_producao_room_new RENAME TO itens_producao")
                criarIndices(database)
            }
        }

        private fun criarTabelasRoom(database: SupportSQLiteDatabase) {
            database.execSQL(
                """CREATE TABLE preparos_room_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    nome TEXT COLLATE NOCASE NOT NULL,
                    descricao TEXT NOT NULL,
                    unidade_medida TEXT NOT NULL,
                    dia_da_semana INTEGER NOT NULL,
                    ativo INTEGER NOT NULL)"""
            )
            database.execSQL(
                """CREATE TABLE producoes_room_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    data TEXT NOT NULL,
                    dia_da_semana INTEGER NOT NULL,
                    clientes_atendidos INTEGER NOT NULL,
                    turno TEXT NOT NULL,
                    restaurante_aberto INTEGER NOT NULL,
                    fechada INTEGER NOT NULL)"""
            )
            database.execSQL(
                """CREATE TABLE itens_producao_room_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    producao_id INTEGER NOT NULL,
                    preparo_id INTEGER NOT NULL,
                    quantidade_produzida REAL NOT NULL,
                    quantidade_sobra REAL NOT NULL,
                    acabou_antes_do_fim INTEGER NOT NULL,
                    horario_acabou TEXT,
                    FOREIGN KEY (producao_id) REFERENCES producoes_room_new(id)
                        ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY (preparo_id) REFERENCES preparos_room_new(id)
                        ON UPDATE NO ACTION ON DELETE RESTRICT)"""
            )
        }

        private fun copiarDados(database: SupportSQLiteDatabase) {
            database.execSQL(
                "INSERT INTO preparos_room_new SELECT " +
                    "id, nome, descricao, unidade_medida, dia_da_semana, ativo FROM preparos"
            )
            database.execSQL(
                "INSERT INTO producoes_room_new SELECT " +
                    "id, data, dia_da_semana, clientes_atendidos, turno, " +
                    "restaurante_aberto, fechada FROM producoes"
            )
            database.execSQL(
                "INSERT INTO itens_producao_room_new SELECT " +
                    "id, producao_id, preparo_id, quantidade_produzida, quantidade_sobra, " +
                    "acabou_antes_do_fim, horario_acabou FROM itens_producao"
            )
        }

        private fun criarIndices(database: SupportSQLiteDatabase) {
            database.execSQL(
                "CREATE UNIQUE INDEX index_preparos_nome_dia_da_semana " +
                    "ON preparos (nome, dia_da_semana)"
            )
            database.execSQL(
                "CREATE UNIQUE INDEX index_producoes_data_turno ON producoes (data, turno)"
            )
            database.execSQL(
                "CREATE UNIQUE INDEX index_itens_producao_producao_id_preparo_id " +
                    "ON itens_producao (producao_id, preparo_id)"
            )
            database.execSQL(
                "CREATE INDEX index_itens_producao_preparo_id ON itens_producao (preparo_id)"
            )
        }
    }
}
