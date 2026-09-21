package com.project.semsobra.data.local;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class SemSobraDatabaseMigrationTest {

    private static final String DATABASE_NAME = "semsobra.db";
    private Context context;

    @Before
    public void prepararBancoAntigo() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.deleteDatabase(DATABASE_NAME);

        try (SQLiteDatabase database = context.openOrCreateDatabase(
                DATABASE_NAME,
                Context.MODE_PRIVATE,
                null
        )) {
            database.execSQL(
                    "CREATE TABLE preparos (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "nome TEXT NOT NULL COLLATE NOCASE UNIQUE, " +
                            "descricao TEXT NOT NULL DEFAULT '', " +
                            "unidade_medida TEXT NOT NULL, " +
                            "dia_da_semana INTEGER NOT NULL " +
                            "CHECK (dia_da_semana BETWEEN 0 AND 7)" +
                            ")"
            );

            ContentValues valores = new ContentValues();
            valores.put("nome", "Arroz branco");
            valores.put("descricao", "Preparo existente antes da atualização");
            valores.put("unidade_medida", "kg");
            valores.put("dia_da_semana", 1);
            database.insertOrThrow("preparos", null, valores);
            database.setVersion(1);
        }
    }

    @After
    public void limparBancoDeTeste() {
        SemSobraDatabaseHelper.getInstance(context).close();
        context.deleteDatabase(DATABASE_NAME);
    }

    @Test
    public void deveMigrarDaVersaoUmSemApagarPreparos() {
        SQLiteDatabase database = SemSobraDatabaseHelper.getInstance(context)
                .getWritableDatabase();

        assertEquals(3, database.getVersion());
        assertTrue(tabelaExiste(database, SemSobraDatabaseHelper.TABELA_PRODUCOES));
        assertTrue(tabelaExiste(database, SemSobraDatabaseHelper.TABELA_ITENS_PRODUCAO));
        assertTrue(colunaExiste(database, SemSobraDatabaseHelper.TABELA_PREPAROS, "ativo"));

        try (Cursor cursor = database.query(
                SemSobraDatabaseHelper.TABELA_PREPAROS,
                new String[]{"nome", "ativo"},
                null,
                null,
                null,
                null,
                null
        )) {
            assertTrue(cursor.moveToFirst());
            assertEquals("Arroz branco", cursor.getString(cursor.getColumnIndexOrThrow("nome")));
            assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("ativo")));
        }
    }

    private boolean tabelaExiste(SQLiteDatabase database, String tabela) {
        try (Cursor cursor = database.rawQuery(
                "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?",
                new String[]{tabela}
        )) {
            return cursor.moveToFirst();
        }
    }

    private boolean colunaExiste(SQLiteDatabase database, String tabela, String coluna) {
        try (Cursor cursor = database.rawQuery("PRAGMA table_info(" + tabela + ")", null)) {
            while (cursor.moveToNext()) {
                if (coluna.equals(cursor.getString(cursor.getColumnIndexOrThrow("name")))) {
                    return true;
                }
            }
        }
        return false;
    }
}
