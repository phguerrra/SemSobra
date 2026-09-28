package com.project.semsobra.domain.usecase;

import com.project.semsobra.domain.model.Preparo;
import com.project.semsobra.domain.repository.PreparoRepository;

import org.junit.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertSame;

public class ListarPreparosUseCaseTest {

    @Test
    public void deveDevolverPreparosDoRepositorio() {
        List<Preparo> preparos = List.of(
                new Preparo(10L, "Arroz", "", "kg", 1, 1)
        );
        ListarPreparosUseCase useCase = new ListarPreparosUseCase(
                new RepositorioFalso(preparos)
        );

        assertSame(preparos, useCase.executar());
    }

    private static final class RepositorioFalso implements PreparoRepository {
        private final List<Preparo> preparos;

        private RepositorioFalso(List<Preparo> preparos) {
            this.preparos = preparos;
        }

        @Override
        public List<Preparo> listarTodos() {
            return preparos;
        }

        @Override
        public Optional<Preparo> buscarPorId(long id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long inserir(Preparo preparo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean atualizar(Preparo preparo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean excluir(long id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean inativar(long id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean existeNome(String nome, int diaDaSemana, Long idIgnorado) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean estaEmUsoNoHistorico(long id) {
            throw new UnsupportedOperationException();
        }
    }
}
