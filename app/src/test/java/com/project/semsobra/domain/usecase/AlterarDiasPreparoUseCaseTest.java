package com.project.semsobra.domain.usecase;

import com.project.semsobra.domain.model.Preparo;
import com.project.semsobra.domain.repository.PreparoRepository;

import org.junit.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class AlterarDiasPreparoUseCaseTest {

    @Test
    public void deveAdicionarDiaAoPreparo() {
        RepositorioFalso repository = new RepositorioFalso(preparoComMascara(0b0000001), true);
        AlterarDiasPreparoUseCase useCase = new AlterarDiasPreparoUseCase(repository);

        AlterarDiasPreparoUseCase.Resultado resultado = useCase.executar(10L, 3, true);

        assertEquals(AlterarDiasPreparoUseCase.Status.ATUALIZADO, resultado.getStatus());
        assertEquals(0b0000101, resultado.getDiasSemanaMask());
        assertEquals(0b0000101, repository.mascaraSalva);
    }

    @Test
    public void deveRemoverDiaDoPreparo() {
        RepositorioFalso repository = new RepositorioFalso(preparoComMascara(0b0000101), true);
        AlterarDiasPreparoUseCase useCase = new AlterarDiasPreparoUseCase(repository);

        AlterarDiasPreparoUseCase.Resultado resultado = useCase.executar(10L, 3, false);

        assertEquals(AlterarDiasPreparoUseCase.Status.ATUALIZADO, resultado.getStatus());
        assertEquals(0b0000001, resultado.getDiasSemanaMask());
    }

    @Test
    public void deveInformarQuandoPreparoNaoExiste() {
        RepositorioFalso repository = new RepositorioFalso(Optional.empty(), true);
        AlterarDiasPreparoUseCase useCase = new AlterarDiasPreparoUseCase(repository);

        AlterarDiasPreparoUseCase.Resultado resultado = useCase.executar(10L, 3, true);

        assertEquals(AlterarDiasPreparoUseCase.Status.NAO_ENCONTRADO, resultado.getStatus());
        assertFalse(repository.atualizarFoiChamado);
    }

    @Test
    public void deveInformarQuandoAtualizacaoFalhar() {
        RepositorioFalso repository = new RepositorioFalso(preparoComMascara(0b0000001), false);
        AlterarDiasPreparoUseCase useCase = new AlterarDiasPreparoUseCase(repository);

        AlterarDiasPreparoUseCase.Resultado resultado = useCase.executar(10L, 3, true);

        assertEquals(AlterarDiasPreparoUseCase.Status.NAO_ENCONTRADO, resultado.getStatus());
        assertTrue(repository.atualizarFoiChamado);
        assertEquals(0b0000001, resultado.getDiasSemanaMask());
    }

    @Test
    public void deveRejeitarDiaInvalidoSemConsultarRepositorio() {
        RepositorioFalso repository = new RepositorioFalso(preparoComMascara(0b0000001), true);
        AlterarDiasPreparoUseCase useCase = new AlterarDiasPreparoUseCase(repository);

        assertThrows(IllegalArgumentException.class, () -> useCase.executar(10L, 8, true));
        assertFalse(repository.buscarFoiChamado);
        assertFalse(repository.atualizarFoiChamado);
    }

    private Optional<Preparo> preparoComMascara(int mascara) {
        return Optional.of(new Preparo(10L, "Arroz", "", "kg", 1, mascara));
    }

    private static final class RepositorioFalso implements PreparoRepository {
        private final Optional<Preparo> preparo;
        private final boolean atualizarComSucesso;
        private boolean buscarFoiChamado;
        private boolean atualizarFoiChamado;
        private int mascaraSalva;

        private RepositorioFalso(Optional<Preparo> preparo, boolean atualizarComSucesso) {
            this.preparo = preparo;
            this.atualizarComSucesso = atualizarComSucesso;
        }

        @Override
        public Optional<Preparo> buscarPorId(long id) {
            buscarFoiChamado = true;
            return preparo;
        }

        @Override
        public boolean atualizarDias(long id, int diasSemanaMask) {
            atualizarFoiChamado = true;
            mascaraSalva = diasSemanaMask;
            return atualizarComSucesso;
        }

        @Override
        public List<Preparo> listarTodos() {
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
