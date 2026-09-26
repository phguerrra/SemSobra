package com.project.semsobra.domain.repository;

import com.project.semsobra.domain.model.Preparo;

import java.util.List;
import java.util.Optional;

public interface PreparoRepository {

    List<Preparo> listarTodos();

    Optional<Preparo> buscarPorId(long id);

    long inserir(Preparo preparo);

    boolean atualizar(Preparo preparo);

    boolean excluir(long id);

    boolean inativar(long id);

    default boolean atualizarDias(long id, int diasSemanaMask) {
        return false;
    }

    boolean existeNome(String nome, int diaDaSemana, Long idIgnorado);

    boolean estaEmUsoNoHistorico(long id);
}
