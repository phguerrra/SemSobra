package com.project.semsobra.domain.model;

public final class Preparo {

    public static final int TODOS_OS_DIAS = 0;

    private final long id;
    private final String nome;
    private final String descricao;
    private final String unidadeMedida;
    private final int diaDaSemana;

    public Preparo(String nome, String descricao, String unidadeMedida, int diaDaSemana) {
        this(0, nome, descricao, unidadeMedida, diaDaSemana);
    }

    public Preparo(long id, String nome, String descricao, String unidadeMedida, int diaDaSemana) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("O nome do preparo é obrigatório");
        }
        if (diaDaSemana < TODOS_OS_DIAS || diaDaSemana > 7) {
            throw new IllegalArgumentException("O dia da semana deve estar entre 0 e 7");
        }

        this.id = id;
        this.nome = nome.trim();
        this.descricao = descricao == null ? "" : descricao.trim();
        this.unidadeMedida = unidadeMedida == null || unidadeMedida.isBlank()
                ? "kg"
                : unidadeMedida.trim();
        this.diaDaSemana = diaDaSemana;
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
}
