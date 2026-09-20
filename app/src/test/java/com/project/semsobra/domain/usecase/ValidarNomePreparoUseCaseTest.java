package com.project.semsobra.domain.usecase;

import com.project.semsobra.domain.model.Preparo;
import com.project.semsobra.domain.repository.PreparoRepository;

import org.junit.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ValidarNomePreparoUseCaseTest {

    @Test
    public void deveIndicarNomeDisponivel() {
        RepositorioFalso repository = new RepositorioFalso(false);
        ValidarNomePreparoUseCase useCase = new ValidarNomePreparoUseCase(repository);

        assertTrue(useCase.estaDisponivel("  Arroz branco  ", null));
        assertEquals("Arroz branco", repository.nomeConsultado);
    }

    @Test
    public void deveIndicarNomeIndisponivel() {
        RepositorioFalso repository = new RepositorioFalso(true);
        ValidarNomePreparoUseCase useCase = new ValidarNomePreparoUseCase(repository);

        assertFalse(useCase.estaDisponivel("Arroz branco", null));
    }

    @Test
    public void deveIgnorarOProprioRegistroDuranteEdicao() {
        RepositorioFalso repository = new RepositorioFalso(false);
        ValidarNomePreparoUseCase useCase = new ValidarNomePreparoUseCase(repository);

        assertTrue(useCase.estaDisponivel("Arroz branco", 15L));
        assertEquals(Long.valueOf(15L), repository.idIgnorado);
    }

    @Test
    public void deveRejeitarNomeVazioSemConsultarRepositorio() {
        RepositorioFalso repository = new RepositorioFalso(false);
        ValidarNomePreparoUseCase useCase = new ValidarNomePreparoUseCase(repository);

        assertFalse(useCase.estaDisponivel(" ", null));
        assertEquals(0, repository.quantidadeConsultas);
    }

    private static final class RepositorioFalso implements PreparoRepository {

        private final boolean nomeEmUso;
        private String nomeConsultado;
        private Long idIgnorado;
        private int quantidadeConsultas;

        private RepositorioFalso(boolean nomeEmUso) {
            this.nomeEmUso = nomeEmUso;
        }

        @Override
        public boolean existeNome(String nome, Long idIgnorado) {
            this.nomeConsultado = nome;
            this.idIgnorado = idIgnorado;
            quantidadeConsultas++;
            return nomeEmUso;
        }

        @Override
        public List<Preparo> listarTodos() {
            throw new UnsupportedOperationException();
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
        public boolean estaEmUsoNoHistorico(long id) {
            throw new UnsupportedOperationException();
        }
    }
}
