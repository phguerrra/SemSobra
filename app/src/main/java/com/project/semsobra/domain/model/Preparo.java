package com.project.semsobra.domain.model;

import java.util.Locale;

public final class Preparo {

    public static final int TODOS_OS_DIAS = 0;

    private final long id;
    private final String nome;
    private final String descricao;
    private final String unidadeMedida;
    private final int diaDaSemana;
    private final int diasSemanaMask;

    public Preparo(String nome, String descricao, String unidadeMedida, int diaDaSemana) {
        this(0, nome, descricao, unidadeMedida, diaDaSemana);
    }

    public Preparo(long id, String nome, String descricao, String unidadeMedida, int diaDaSemana) {
        this(id, nome, descricao, unidadeMedida, diaDaSemana,
                diaDaSemana == TODOS_OS_DIAS ? 0 : 1 << (diaDaSemana - 1));
    }

    public Preparo(long id, String nome, String descricao, String unidadeMedida,
                   int diaDaSemana, int diasSemanaMask) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("O nome do preparo é obrigatório");
        }
        if (diaDaSemana < TODOS_OS_DIAS || diaDaSemana > 7) {
            throw new IllegalArgumentException("O dia da semana deve estar entre 0 e 7");
        }

        this.id = id;
        this.nome = normalizarNome(nome);
        this.descricao = descricao == null ? "" : descricao.trim();
        this.unidadeMedida = unidadeMedida == null || unidadeMedida.isBlank()
                ? "kg"
                : unidadeMedida.trim();
        this.diaDaSemana = diaDaSemana;
        this.diasSemanaMask = diasSemanaMask;
    }

    public long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getUnidadeMedida() {
        return unidadeMedida;
    }

    public int getDiaDaSemana() {
        return diaDaSemana;
    }

    public int getDiasSemanaMask() {
        return diasSemanaMask;
    }

    public static String normalizarNome(String nome) {
        if (nome == null) {
            return "";
        }
        String semEspacosDuplicados = nome.trim().replaceAll("\\s+", " ");
        if (semEspacosDuplicados.isEmpty()) {
            return "";
        }
        String minusculo = semEspacosDuplicados.toLowerCase(Locale.forLanguageTag("pt-BR"));
        return minusculo.substring(0, 1).toUpperCase(Locale.forLanguageTag("pt-BR")) +
                minusculo.substring(1);
    }
}
