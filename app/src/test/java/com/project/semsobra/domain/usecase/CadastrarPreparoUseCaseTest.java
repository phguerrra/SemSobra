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

public class CadastrarPreparoUseCaseTest {

    @Test
    public void deveCadastrarPreparoComDadosNormalizados() {
        RepositorioFalso repository = new RepositorioFalso(false, 10L);
        CadastrarPreparoUseCase useCase = criarUseCase(repository);

        CadastrarPreparoUseCase.Resultado resultado = useCase.executar(
                "  arroz   branco  ", "  Com alho  ", " kg ", 2
        );

        assertEquals(CadastrarPreparoUseCase.Resultado.CADASTRADO, resultado);
        assertTrue(repository.inserirFoiChamado);
        assertEquals("Arroz branco", repository.preparoInserido.getNome());
        assertEquals("Com alho", repository.preparoInserido.getDescricao());
        assertEquals("kg", repository.preparoInserido.getUnidadeMedida());
    }

    @Test
    public void deveRejeitarNomeDuplicadoSemInserir() {
        RepositorioFalso repository = new RepositorioFalso(true, 10L);
        CadastrarPreparoUseCase useCase = criarUseCase(repository);

        CadastrarPreparoUseCase.Resultado resultado = useCase.executar(
                "Arroz", "", "kg", 2
        );

        assertEquals(CadastrarPreparoUseCase.Resultado.NOME_DUPLICADO, resultado);
        assertFalse(repository.inserirFoiChamado);
    }

    @Test
    public void deveRejeitarNomeVazio() {
        RepositorioFalso repository = new RepositorioFalso(false, 10L);
        CadastrarPreparoUseCase useCase = criarUseCase(repository);

        assertThrows(
                IllegalArgumentException.class,
                () -> useCase.executar(" ", "", "kg", 2)
        );
        assertFalse(repository.inserirFoiChamado);
    }

    @Test
    public void deveFalharQuandoRepositorioNaoGerarId() {
        RepositorioFalso repository = new RepositorioFalso(false, 0L);
        CadastrarPreparoUseCase useCase = criarUseCase(repository);

        assertThrows(
                IllegalStateException.class,
                () -> useCase.executar("Arroz", "", "kg", 2)
        );
    }

    private CadastrarPreparoUseCase criarUseCase(RepositorioFalso repository) {
        return new CadastrarPreparoUseCase(
                repository,
                new ValidarNomePreparoUseCase(repository)
        );
    }

    private static final class RepositorioFalso implements PreparoRepository {
        private final boolean nomeDuplicado;
        private final long idGerado;
        private boolean inserirFoiChamado;
        private Preparo preparoInserido;

        private RepositorioFalso(boolean nomeDuplicado, long idGerado) {
            this.nomeDuplicado = nomeDuplicado;
            this.idGerado = idGerado;
        }

        @Override
        public long inserir(Preparo preparo) {
            inserirFoiChamado = true;
            preparoInserido = preparo;
            return idGerado;
        }

        @Override
        public boolean existeNome(String nome, int diaDaSemana, Long idIgnorado) {
            return nomeDuplicado;
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
