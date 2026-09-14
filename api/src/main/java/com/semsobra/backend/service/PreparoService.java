package com.semsobra.backend.service;

import com.semsobra.backend.dto.PreparoRequest;
import com.semsobra.backend.dto.PreparoResponse;
import com.semsobra.backend.entity.Preparo;
import com.semsobra.backend.exception.RecursoDuplicadoException;
import com.semsobra.backend.exception.RecursoNaoEncontradoException;
import com.semsobra.backend.repository.PreparoRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PreparoService {

    private final PreparoRepository repository;

    public PreparoService(PreparoRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<PreparoResponse> listar(String nome) {
        List<Preparo> preparos;

        if (nome == null || nome.isBlank()) {
            preparos = repository.findAll(Sort.by(Sort.Direction.ASC, "nome"));
        } else {
            preparos = repository.findByNomeContainingIgnoreCaseOrderByNomeAsc(nome.trim());
        }

        return preparos.stream().map(this::converterParaResponse).toList();
    }

    @Transactional(readOnly = true)
    public PreparoResponse buscarPorId(Long id) {
        return converterParaResponse(buscarEntidade(id));
    }

    @Transactional
    public PreparoResponse criar(PreparoRequest request) {
        String nome = request.nome().trim();

        validarNomeDisponivel(nome, null);

        Preparo preparo = new Preparo(nome, normalizarDescricao(request.descricao()), request.unidadeMedida().trim());

        Preparo preparoSalvo = repository.save(preparo);

        return converterParaResponse(preparoSalvo);
    }

    @Transactional
    public PreparoResponse atualizar(Long id, PreparoRequest request){
        Preparo preparo = buscarEntidade(id);
        String nome = request.nome().trim();

        validarNomeDisponivel(nome, id);

        preparo.setNome(nome);
        preparo.setDescricao(normalizarDescricao(request.descricao())
        );
        preparo.setUnidadeMedida(request.unidadeMedida().trim()
        );

        Preparo preparoSalvo = repository.save(preparo);

        return converterParaResponse(preparoSalvo);
    }

    @Transactional
    public void excluir(Long id) {
        Preparo preparo = buscarEntidade(id);
        repository.delete(preparo);
    }

    private Preparo buscarEntidade(Long id) {
        return repository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Preparo com ID " + id + " não encontrado"));
    }

    private PreparoResponse converterParaResponse(Preparo preparo) {
        return new PreparoResponse(preparo.getId(), preparo.getNome(), preparo.getDescricao(), preparo.getUnidadeMedida());
    }

    private String normalizarDescricao(String descricao) {
        return descricao == null ? "" : descricao.trim();
    }

    private void validarNomeDisponivel(String nome, Long idAtual){
        boolean nomeEmUso;

        if (idAtual == null) {
            nomeEmUso = repository.existsByNomeIgnoreCase(nome);
        } else {
            nomeEmUso = repository.existsByNomeIgnoreCaseAndIdNot(nome, idAtual);
        }

        if (nomeEmUso) {
            throw new RecursoDuplicadoException("Já existe um preparo com o nome " + nome);
        }
    }
}
