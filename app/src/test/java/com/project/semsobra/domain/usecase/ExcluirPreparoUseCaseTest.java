package com.project.semsobra.domain.usecase;

import com.project.semsobra.domain.model.Preparo;
import com.project.semsobra.domain.repository.PreparoRepository;

import org.junit.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ExcluirPreparoUseCaseTest {

    @Test
    public void deveExcluirPreparoSemHistorico() {
        RepositorioFalso repository = new RepositorioFalso(false, true);
        ExcluirPreparoUseCase useCase = new ExcluirPreparoUseCase(repository);

        assertEquals(ExcluirPreparoUseCase.Resultado.EXCLUIDO, useCase.executar(10L));
        assertTrue(repository.excluirFoiChamado);
    }

    @Test
    public void deveInativarPreparoUsadoNoHistorico() {
        RepositorioFalso repository = new RepositorioFalso(true, true);
        ExcluirPreparoUseCase useCase = new ExcluirPreparoUseCase(repository);

        assertEquals(
                ExcluirPreparoUseCase.Resultado.INATIVADO_POR_HISTORICO,
                useCase.executar(10L)
        );
        assertFalse(repository.excluirFoiChamado);
        assertTrue(repository.inativarFoiChamado);
    }

    @Test
    public void deveInformarQuandoPreparoNaoExiste() {
        RepositorioFalso repository = new RepositorioFalso(false, false);
        ExcluirPreparoUseCase useCase = new ExcluirPreparoUseCase(repository);

        assertEquals(
                ExcluirPreparoUseCase.Resultado.NAO_ENCONTRADO,
                useCase.executar(10L)
        );
    }

    @Test
    public void deveRejeitarIdInvalidoSemConsultarRepositorio() {
        RepositorioFalso repository = new RepositorioFalso(false, true);
        ExcluirPreparoUseCase useCase = new ExcluirPreparoUseCase(repository);

        assertEquals(
                ExcluirPreparoUseCase.Resultado.NAO_ENCONTRADO,
                useCase.executar(0L)
        );
        assertFalse(repository.historicoFoiConsultado);
        assertFalse(repository.excluirFoiChamado);
        assertFalse(repository.inativarFoiChamado);
    }

    private static final class RepositorioFalso implements PreparoRepository {

        private final boolean emUsoNoHistorico;
        private final boolean excluirComSucesso;
        private boolean historicoFoiConsultado;
        private boolean excluirFoiChamado;
        private boolean inativarFoiChamado;

        private RepositorioFalso(boolean emUsoNoHistorico, boolean excluirComSucesso) {
            this.emUsoNoHistorico = emUsoNoHistorico;
            this.excluirComSucesso = excluirComSucesso;
        }

        @Override
        public boolean estaEmUsoNoHistorico(long id) {
            historicoFoiConsultado = true;
            return emUsoNoHistorico;
        }

        @Override
        public boolean excluir(long id) {
            excluirFoiChamado = true;
            return excluirComSucesso;
        }

        @Override
        public boolean inativar(long id) {
            inativarFoiChamado = true;
            return excluirComSucesso;
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
        public boolean existeNome(String nome, Long idIgnorado) {
            throw new UnsupportedOperationException();
        }
    }
}
