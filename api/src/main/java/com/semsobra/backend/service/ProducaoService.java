package com.semsobra.backend.service;

import com.semsobra.backend.dto.ItemProducaoRequest;
import com.semsobra.backend.dto.ItemProducaoResponse;
import com.semsobra.backend.dto.ProducaoRequest;
import com.semsobra.backend.dto.ProducaoResponse;
import com.semsobra.backend.entity.ItemProducao;
import com.semsobra.backend.entity.Preparo;
import com.semsobra.backend.entity.ProducaoDia;
import com.semsobra.backend.exception.RecursoDuplicadoException;
import com.semsobra.backend.exception.RecursoNaoEncontradoException;
import com.semsobra.backend.repository.PreparoRepository;
import com.semsobra.backend.repository.ProducaoDiaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ProducaoService {

    private final ProducaoDiaRepository producaoRepository;
    private final PreparoRepository preparoRepository;

    public ProducaoService(ProducaoDiaRepository producaoRepository, PreparoRepository preparoRepository) {
        this.producaoRepository = producaoRepository;
        this.preparoRepository = preparoRepository;
    }

    @Transactional
    public ProducaoResponse criar(ProducaoRequest request) {
        validarProducaoDisponivel(request);

        ProducaoDia producao = new ProducaoDia(request.data(), request.turno());
        Set<Long> preparosAdicionados = new HashSet<>();

        for (ItemProducaoRequest itemRequest : request.itens()) {
            if (!preparosAdicionados.add(itemRequest.preparoId())) {
                throw new RecursoDuplicadoException("O preparo com ID " + itemRequest.preparoId() + " está repetido na produção");
            }

            Preparo preparo = preparoRepository.findById(itemRequest.preparoId()).orElseThrow(
                    () -> new RecursoNaoEncontradoException("Preparo com ID " + itemRequest.preparoId() + " não encontrado")
            );

            producao.adicionarItem(new ItemProducao(preparo, itemRequest.quantidadeProduzida()));
        }

        return converterParaResponse(producaoRepository.save(producao));
    }

    @Transactional(readOnly = true)
    public List<ProducaoResponse> listar() {
        return producaoRepository.findAllByOrderByDataDescTurnoAsc().stream().map(this::converterParaResponse).toList();
    }

    @Transactional(readOnly = true)
    public ProducaoResponse buscarPorId(Long id) {
        return converterParaResponse(buscarEntidade(id));
    }

    private ProducaoDia buscarEntidade(Long id) {
        return producaoRepository.findById(id).orElseThrow(
                () -> new RecursoNaoEncontradoException("Produção com ID " + id + " não encontrada")
        );
    }

    private void validarProducaoDisponivel(ProducaoRequest request) {
        if (producaoRepository.existsByDataAndTurno(request.data(), request.turno())) {
            throw new RecursoDuplicadoException("Já existe uma produção para " + request.data() + " no turno " + request.turno());
        }
    }

    private ProducaoResponse converterParaResponse(ProducaoDia producao) {
        List<ItemProducaoResponse> itens = producao.getItens().stream().map(this::converterItemParaResponse).toList();

        return new ProducaoResponse(
                producao.getId(),
                producao.getData(),
                producao.getTurno(),
                producao.getClientesAtendidos(),
                producao.isRestauranteAberto(),
                producao.isFechado(),
                itens
        );
    }

    private ItemProducaoResponse converterItemParaResponse(ItemProducao item) {
        return new ItemProducaoResponse(
                item.getId(),
                item.getPreparo().getId(),
                item.getPreparo().getNome(),
                item.getQuantidadeProduzida(),
                item.getQuantidadeSobra(),
                item.isAcabouAntesDoFim(),
                item.getHorarioAcabou()
        );
    }
}
