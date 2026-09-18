package com.project.semsobra.data.local;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public final class SemSobraDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "semsobra.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABELA_PREPAROS = "preparos";
    public static final String COLUNA_ID = "id";
    public static final String COLUNA_NOME = "nome";
    public static final String COLUNA_DESCRICAO = "descricao";
    public static final String COLUNA_UNIDADE_MEDIDA = "unidade_medida";
    public static final String COLUNA_DIA_DA_SEMANA = "dia_da_semana";

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
        database.execSQL(
                "CREATE TABLE " + TABELA_PREPAROS + " (" +
                        COLUNA_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COLUNA_NOME + " TEXT NOT NULL COLLATE NOCASE UNIQUE, " +
                        COLUNA_DESCRICAO + " TEXT NOT NULL DEFAULT '', " +
                        COLUNA_UNIDADE_MEDIDA + " TEXT NOT NULL, " +
                        COLUNA_DIA_DA_SEMANA + " INTEGER NOT NULL " +
                        "CHECK (" + COLUNA_DIA_DA_SEMANA + " BETWEEN 0 AND 7)" +
                        ")"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        // As próximas alterações do banco serão adicionadas aqui como migrações.
    }
}
