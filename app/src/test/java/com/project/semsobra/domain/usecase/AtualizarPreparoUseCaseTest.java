package com.project.semsobra.domain.usecase;

import com.project.semsobra.domain.model.Preparo;
import com.project.semsobra.domain.repository.PreparoRepository;

import org.junit.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AtualizarPreparoUseCaseTest {

    @Test
    public void deveAtualizarPreparoPreservandoDiasSelecionados() {
        int diasSelecionados = 0b0010101;
        RepositorioFalso repository = new RepositorioFalso(
                Optional.of(new Preparo(10L, "Arroz", "", "kg", 1, diasSelecionados)),
                false,
                true
        );
        AtualizarPreparoUseCase useCase = criarUseCase(repository);

        AtualizarPreparoUseCase.Resultado resultado = useCase.executar(
                10L, "  arroz integral ", "  Com alho  ", " kg ", 2
        );

        assertEquals(AtualizarPreparoUseCase.Resultado.ATUALIZADO, resultado);
        assertEquals("Arroz integral", repository.preparoAtualizado.getNome());
        assertEquals("Com alho", repository.preparoAtualizado.getDescricao());
        assertEquals(diasSelecionados, repository.preparoAtualizado.getDiasSemanaMask());
        assertEquals(Long.valueOf(10L), repository.idIgnoradoNaValidacao);
    }

    @Test
    public void deveRejeitarNomeDuplicadoSemAtualizar() {
        RepositorioFalso repository = new RepositorioFalso(
                Optional.of(new Preparo(10L, "Arroz", "", "kg", 2)),
                true,
                true
        );
        AtualizarPreparoUseCase useCase = criarUseCase(repository);

        AtualizarPreparoUseCase.Resultado resultado = useCase.executar(
                10L, "Feijão", "", "kg", 2
        );

        assertEquals(AtualizarPreparoUseCase.Resultado.NOME_DUPLICADO, resultado);
        assertFalse(repository.atualizarFoiChamado);
    }

    @Test
    public void deveInformarQuandoPreparoNaoExiste() {
        RepositorioFalso repository = new RepositorioFalso(Optional.empty(), false, true);
        AtualizarPreparoUseCase useCase = criarUseCase(repository);

        AtualizarPreparoUseCase.Resultado resultado = useCase.executar(
                10L, "Arroz", "", "kg", 2
        );

        assertEquals(AtualizarPreparoUseCase.Resultado.NAO_ENCONTRADO, resultado);
        assertFalse(repository.atualizarFoiChamado);
    }

    @Test
    public void deveInformarQuandoAtualizacaoNaoAlterarRegistro() {
        RepositorioFalso repository = new RepositorioFalso(
                Optional.of(new Preparo(10L, "Arroz", "", "kg", 2)),
                false,
                false
        );
        AtualizarPreparoUseCase useCase = criarUseCase(repository);

        AtualizarPreparoUseCase.Resultado resultado = useCase.executar(
                10L, "Arroz", "", "kg", 2
        );

        assertEquals(AtualizarPreparoUseCase.Resultado.NAO_ENCONTRADO, resultado);
        assertTrue(repository.atualizarFoiChamado);
    }

    private AtualizarPreparoUseCase criarUseCase(RepositorioFalso repository) {
        return new AtualizarPreparoUseCase(
                repository,
                new ValidarNomePreparoUseCase(repository)
        );
    }

    private static final class RepositorioFalso implements PreparoRepository {
        private final Optional<Preparo> preparoExistente;
        private final boolean nomeDuplicado;
        private final boolean atualizarComSucesso;
        private boolean atualizarFoiChamado;
        private Preparo preparoAtualizado;
        private Long idIgnoradoNaValidacao;

        private RepositorioFalso(
                Optional<Preparo> preparoExistente,
                boolean nomeDuplicado,
                boolean atualizarComSucesso
        ) {
            this.preparoExistente = preparoExistente;
            this.nomeDuplicado = nomeDuplicado;
            this.atualizarComSucesso = atualizarComSucesso;
        }

        @Override
        public Optional<Preparo> buscarPorId(long id) {
            return preparoExistente;
        }

        @Override
        public boolean atualizar(Preparo preparo) {
            atualizarFoiChamado = true;
            preparoAtualizado = preparo;
            return atualizarComSucesso;
        }

        @Override
        public boolean existeNome(String nome, int diaDaSemana, Long idIgnorado) {
            idIgnoradoNaValidacao = idIgnorado;
            return nomeDuplicado;
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
