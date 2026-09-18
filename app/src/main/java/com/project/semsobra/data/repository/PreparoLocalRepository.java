package com.project.semsobra.data.repository;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.project.semsobra.data.local.SemSobraDatabaseHelper;
import com.project.semsobra.domain.model.Preparo;
import com.project.semsobra.domain.repository.PreparoRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class PreparoLocalRepository implements PreparoRepository {

    private final SemSobraDatabaseHelper databaseHelper;

    public PreparoLocalRepository(Context context) {
        databaseHelper = SemSobraDatabaseHelper.getInstance(context);
    }

    @Override
    public List<Preparo> listarTodos() {
        SQLiteDatabase database = databaseHelper.getReadableDatabase();
        List<Preparo> preparos = new ArrayList<>();

        try (Cursor cursor = database.query(
                SemSobraDatabaseHelper.TABELA_PREPAROS,
                null,
                null,
                null,
                null,
                null,
                SemSobraDatabaseHelper.COLUNA_NOME + " COLLATE NOCASE ASC"
        )) {
            while (cursor.moveToNext()) {
                preparos.add(mapearPreparo(cursor));
            }
        }

        return preparos;
    }

    @Override
    public Optional<Preparo> buscarPorId(long id) {
        SQLiteDatabase database = databaseHelper.getReadableDatabase();

        try (Cursor cursor = database.query(
                SemSobraDatabaseHelper.TABELA_PREPAROS,
                null,
                SemSobraDatabaseHelper.COLUNA_ID + " = ?",
                new String[]{String.valueOf(id)},
                null,
                null,
                null
        )) {
            if (cursor.moveToFirst()) {
                return Optional.of(mapearPreparo(cursor));
            }
        }

        return Optional.empty();
    }

    @Override
    public long inserir(Preparo preparo) {
        return databaseHelper.getWritableDatabase().insertOrThrow(
                SemSobraDatabaseHelper.TABELA_PREPAROS,
                null,
                criarValores(preparo)
        );
    }

    @Override
    public boolean atualizar(Preparo preparo) {
        if (preparo.getId() <= 0) {
            throw new IllegalArgumentException("O preparo precisa ter um ID para ser atualizado");
        }

        int linhasAlteradas = databaseHelper.getWritableDatabase().update(
                SemSobraDatabaseHelper.TABELA_PREPAROS,
                criarValores(preparo),
                SemSobraDatabaseHelper.COLUNA_ID + " = ?",
                new String[]{String.valueOf(preparo.getId())}
        );
        return linhasAlteradas > 0;
    }

    @Override
    public boolean excluir(long id) {
        int linhasExcluidas = databaseHelper.getWritableDatabase().delete(
                SemSobraDatabaseHelper.TABELA_PREPAROS,
                SemSobraDatabaseHelper.COLUNA_ID + " = ?",
                new String[]{String.valueOf(id)}
        );
        return linhasExcluidas > 0;
    }

    @Override
    public boolean existeNome(String nome, Long idIgnorado) {
        SQLiteDatabase database = databaseHelper.getReadableDatabase();
        String selecao = SemSobraDatabaseHelper.COLUNA_NOME + " = ?";
        List<String> argumentos = new ArrayList<>();
        argumentos.add(nome.trim());

        if (idIgnorado != null) {
            selecao += " AND " + SemSobraDatabaseHelper.COLUNA_ID + " <> ?";
            argumentos.add(String.valueOf(idIgnorado));
        }

        try (Cursor cursor = database.query(
                SemSobraDatabaseHelper.TABELA_PREPAROS,
                new String[]{SemSobraDatabaseHelper.COLUNA_ID},
                selecao,
                argumentos.toArray(new String[0]),
                null,
                null,
                null,
                "1"
        )) {
            return cursor.moveToFirst();
        }
    }

    private ContentValues criarValores(Preparo preparo) {
        ContentValues valores = new ContentValues();
        valores.put(SemSobraDatabaseHelper.COLUNA_NOME, preparo.getNome());
        valores.put(SemSobraDatabaseHelper.COLUNA_DESCRICAO, preparo.getDescricao());
        valores.put(SemSobraDatabaseHelper.COLUNA_UNIDADE_MEDIDA, preparo.getUnidadeMedida());
        valores.put(SemSobraDatabaseHelper.COLUNA_DIA_DA_SEMANA, preparo.getDiaDaSemana());
        return valores;
    }

    private Preparo mapearPreparo(Cursor cursor) {
        return new Preparo(
                cursor.getLong(cursor.getColumnIndexOrThrow(SemSobraDatabaseHelper.COLUNA_ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(SemSobraDatabaseHelper.COLUNA_NOME)),
                cursor.getString(cursor.getColumnIndexOrThrow(SemSobraDatabaseHelper.COLUNA_DESCRICAO)),
                cursor.getString(cursor.getColumnIndexOrThrow(SemSobraDatabaseHelper.COLUNA_UNIDADE_MEDIDA)),
                cursor.getInt(cursor.getColumnIndexOrThrow(SemSobraDatabaseHelper.COLUNA_DIA_DA_SEMANA))
        );
    }
}
