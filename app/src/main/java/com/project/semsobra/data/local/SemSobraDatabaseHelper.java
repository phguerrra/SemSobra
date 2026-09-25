package com.project.semsobra.data.local;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public final class SemSobraDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "semsobra.db";
    private static final int DATABASE_VERSION = 4;

    public static final String TABELA_PREPAROS = "preparos";
    public static final String COLUNA_ID = "id";
    public static final String COLUNA_NOME = "nome";
    public static final String COLUNA_DESCRICAO = "descricao";
    public static final String COLUNA_UNIDADE_MEDIDA = "unidade_medida";
    public static final String COLUNA_DIA_DA_SEMANA = "dia_da_semana";
    public static final String COLUNA_ATIVO = "ativo";

    public static final String TABELA_PRODUCOES = "producoes";
    public static final String COLUNA_PRODUCAO_DATA = "data";
    public static final String COLUNA_PRODUCAO_DIA_DA_SEMANA = "dia_da_semana";
    public static final String COLUNA_PRODUCAO_CLIENTES_ATENDIDOS = "clientes_atendidos";
    public static final String COLUNA_PRODUCAO_TURNO = "turno";
    public static final String COLUNA_PRODUCAO_RESTAURANTE_ABERTO = "restaurante_aberto";
    public static final String COLUNA_PRODUCAO_FECHADA = "fechada";

    public static final String TABELA_ITENS_PRODUCAO = "itens_producao";
    public static final String COLUNA_ITEM_PRODUCAO_ID = "producao_id";
    public static final String COLUNA_ITEM_PREPARO_ID = "preparo_id";
    public static final String COLUNA_ITEM_QUANTIDADE_PRODUZIDA = "quantidade_produzida";
    public static final String COLUNA_ITEM_QUANTIDADE_SOBRA = "quantidade_sobra";
    public static final String COLUNA_ITEM_ACABOU_ANTES_DO_FIM = "acabou_antes_do_fim";
    public static final String COLUNA_ITEM_HORARIO_ACABOU = "horario_acabou";

    private static SemSobraDatabaseHelper instance;

    private SemSobraDatabaseHelper(Context context) {
        super(context.getApplicationContext(), DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static synchronized SemSobraDatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new SemSobraDatabaseHelper(context);
        }
        return instance;
    }

    @Override
    public void onConfigure(SQLiteDatabase database) {
        super.onConfigure(database);
        database.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        criarTabelaPreparos(database);
        criarTabelasDeProducao(database);
    }

    private void criarTabelaPreparos(SQLiteDatabase database) {
        database.execSQL(
                "CREATE TABLE " + TABELA_PREPAROS + " (" +
                        COLUNA_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COLUNA_NOME + " TEXT NOT NULL COLLATE NOCASE, " +
                        COLUNA_DESCRICAO + " TEXT NOT NULL DEFAULT '', " +
                        COLUNA_UNIDADE_MEDIDA + " TEXT NOT NULL, " +
                        COLUNA_DIA_DA_SEMANA + " INTEGER NOT NULL " +
                        "CHECK (" + COLUNA_DIA_DA_SEMANA + " BETWEEN 0 AND 7), " +
                        COLUNA_ATIVO + " INTEGER NOT NULL DEFAULT 1 " +
                        "CHECK (" + COLUNA_ATIVO + " IN (0, 1)), " +
                        "UNIQUE (" + COLUNA_NOME + ", " + COLUNA_DIA_DA_SEMANA + ")" +
                        ")"
        );
    }

    private void criarTabelasDeProducao(SQLiteDatabase database) {
        database.execSQL(
                "CREATE TABLE " + TABELA_PRODUCOES + " (" +
                        COLUNA_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COLUNA_PRODUCAO_DATA + " TEXT NOT NULL, " +
                        COLUNA_PRODUCAO_DIA_DA_SEMANA + " INTEGER NOT NULL " +
                        "CHECK (" + COLUNA_PRODUCAO_DIA_DA_SEMANA + " BETWEEN 1 AND 7), " +
                        COLUNA_PRODUCAO_CLIENTES_ATENDIDOS + " INTEGER NOT NULL DEFAULT 0 " +
                        "CHECK (" + COLUNA_PRODUCAO_CLIENTES_ATENDIDOS + " >= 0), " +
                        COLUNA_PRODUCAO_TURNO + " TEXT NOT NULL, " +
                        COLUNA_PRODUCAO_RESTAURANTE_ABERTO + " INTEGER NOT NULL DEFAULT 1 " +
                        "CHECK (" + COLUNA_PRODUCAO_RESTAURANTE_ABERTO + " IN (0, 1)), " +
                        COLUNA_PRODUCAO_FECHADA + " INTEGER NOT NULL DEFAULT 0 " +
                        "CHECK (" + COLUNA_PRODUCAO_FECHADA + " IN (0, 1)), " +
                        "UNIQUE (" + COLUNA_PRODUCAO_DATA + ", " + COLUNA_PRODUCAO_TURNO + ")" +
                        ")"
        );

        database.execSQL(
                "CREATE TABLE " + TABELA_ITENS_PRODUCAO + " (" +
                        COLUNA_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COLUNA_ITEM_PRODUCAO_ID + " INTEGER NOT NULL, " +
                        COLUNA_ITEM_PREPARO_ID + " INTEGER NOT NULL, " +
                        COLUNA_ITEM_QUANTIDADE_PRODUZIDA + " REAL NOT NULL " +
                        "CHECK (" + COLUNA_ITEM_QUANTIDADE_PRODUZIDA + " >= 0), " +
                        COLUNA_ITEM_QUANTIDADE_SOBRA + " REAL NOT NULL DEFAULT 0 " +
                        "CHECK (" + COLUNA_ITEM_QUANTIDADE_SOBRA + " >= 0 AND " +
                        COLUNA_ITEM_QUANTIDADE_SOBRA + " <= " + COLUNA_ITEM_QUANTIDADE_PRODUZIDA + "), " +
                        COLUNA_ITEM_ACABOU_ANTES_DO_FIM + " INTEGER NOT NULL DEFAULT 0 " +
                        "CHECK (" + COLUNA_ITEM_ACABOU_ANTES_DO_FIM + " IN (0, 1)), " +
                        COLUNA_ITEM_HORARIO_ACABOU + " TEXT, " +
                        "FOREIGN KEY (" + COLUNA_ITEM_PRODUCAO_ID + ") REFERENCES " +
                        TABELA_PRODUCOES + "(" + COLUNA_ID + ") ON DELETE CASCADE, " +
                        "FOREIGN KEY (" + COLUNA_ITEM_PREPARO_ID + ") REFERENCES " +
                        TABELA_PREPAROS + "(" + COLUNA_ID + ") ON DELETE RESTRICT, " +
                        "UNIQUE (" + COLUNA_ITEM_PRODUCAO_ID + ", " + COLUNA_ITEM_PREPARO_ID + ")" +
                        ")"
        );

        database.execSQL(
                "CREATE INDEX indice_itens_producao_preparo ON " + TABELA_ITENS_PRODUCAO +
                        " (" + COLUNA_ITEM_PREPARO_ID + ")"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            criarTabelasDeProducao(database);
        }
        if (oldVersion < 3) {
            database.execSQL(
                    "ALTER TABLE " + TABELA_PREPAROS +
                            " ADD COLUMN " + COLUNA_ATIVO +
                            " INTEGER NOT NULL DEFAULT 1 " +
                            "CHECK (" + COLUNA_ATIVO + " IN (0, 1))"
            );
        }
        if (oldVersion < 4) {
            migrarUnicidadeDePreparos(database);
        }
    }

    private void migrarUnicidadeDePreparos(SQLiteDatabase database) {
        String tabelaNova = TABELA_PREPAROS + "_nova";
        database.execSQL("PRAGMA defer_foreign_keys = ON");
        database.execSQL(
                "CREATE TABLE " + tabelaNova + " (" +
                        COLUNA_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COLUNA_NOME + " TEXT NOT NULL COLLATE NOCASE, " +
                        COLUNA_DESCRICAO + " TEXT NOT NULL DEFAULT '', " +
                        COLUNA_UNIDADE_MEDIDA + " TEXT NOT NULL, " +
                        COLUNA_DIA_DA_SEMANA + " INTEGER NOT NULL " +
                        "CHECK (" + COLUNA_DIA_DA_SEMANA + " BETWEEN 0 AND 7), " +
                        COLUNA_ATIVO + " INTEGER NOT NULL DEFAULT 1 " +
                        "CHECK (" + COLUNA_ATIVO + " IN (0, 1)), " +
                        "UNIQUE (" + COLUNA_NOME + ", " + COLUNA_DIA_DA_SEMANA + ")" +
                        ")"
        );
        database.execSQL(
                "INSERT INTO " + tabelaNova + " (" +
                        COLUNA_ID + ", " + COLUNA_NOME + ", " + COLUNA_DESCRICAO + ", " +
                        COLUNA_UNIDADE_MEDIDA + ", " + COLUNA_DIA_DA_SEMANA + ", " +
                        COLUNA_ATIVO + ") SELECT " +
                        COLUNA_ID + ", " + COLUNA_NOME + ", " + COLUNA_DESCRICAO + ", " +
                        COLUNA_UNIDADE_MEDIDA + ", " + COLUNA_DIA_DA_SEMANA + ", " +
                        COLUNA_ATIVO + " FROM " + TABELA_PREPAROS
        );
        database.execSQL("DROP TABLE " + TABELA_PREPAROS);
        database.execSQL("ALTER TABLE " + tabelaNova + " RENAME TO " + TABELA_PREPAROS);
    }
}
