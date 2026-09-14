package com.semsobra.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "preparos",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_preparos_nome",
                        columnNames = "nome"
                )
        }
)
public class Preparo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(nullable = false, length = 500)
    private String descricao;

    @Column(name = "unidade_medida", nullable = false, length = 20)
    private String unidadeMedida;

    protected Preparo(){

    }

    public Preparo(String nome, String descricao, String unidadeMedida){
        this.nome = nome;
        this.descricao = descricao;
        this.unidadeMedida = unidadeMedida;
    }

    public Long getId(){
        return id;
    }

    public String getNome(){
        return nome;
    }

    public String getDescricao(){
        return descricao;
    }

    public String getUnidadeMedida(){
        return unidadeMedida;
    }

    public void setNome(String nome){
        this.nome = nome;
    }

    public void setDescricao(String descricao){
        this.descricao = descricao;
    }

    public void setUnidadeMedida(String unidadeMedida){
        this.unidadeMedida = unidadeMedida;
    }
}
