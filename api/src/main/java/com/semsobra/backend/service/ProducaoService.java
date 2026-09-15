package com.semsobra.backend.service;

import com.semsobra.backend.dto.FechamentoItemRequest;
import com.semsobra.backend.dto.FechamentoProducaoRequest;
import com.semsobra.backend.dto.ItemProducaoRequest;
import com.semsobra.backend.dto.ItemProducaoResponse;
import com.semsobra.backend.dto.ProducaoRequest;
import com.semsobra.backend.dto.ProducaoResponse;
import com.semsobra.backend.entity.ItemProducao;
import com.semsobra.backend.entity.Preparo;
import com.semsobra.backend.entity.ProducaoDia;
import com.semsobra.backend.exception.OperacaoInvalidaException;
import com.semsobra.backend.exception.RecursoDuplicadoException;
import com.semsobra.backend.exception.RecursoNaoEncontradoException;
import com.semsobra.backend.repository.PreparoRepository;
import com.semsobra.backend.repository.ProducaoDiaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    @Transactional
    public ProducaoResponse fechar(Long id, FechamentoProducaoRequest request) {
        ProducaoDia producao = buscarEntidade(id);

        if (producao.isFechado()) {
            throw new RecursoDuplicadoException("A produção com ID " + id + " já foi fechada");
        }

        Map<Long, ItemProducao> itensPorId = producao.getItens().stream().collect(
                Collectors.toMap(ItemProducao::getId, Function.identity())
        );
        Set<Long> itensInformados = new HashSet<>();

        for (FechamentoItemRequest itemRequest : request.itens()) {
            if (!itensInformados.add(itemRequest.itemId())) {
                throw new RecursoDuplicadoException("O item com ID " + itemRequest.itemId() + " está repetido no fechamento");
            }

            ItemProducao item = itensPorId.get(itemRequest.itemId());

            if (item == null) {
                throw new OperacaoInvalidaException("O item com ID " + itemRequest.itemId() + " não pertence a esta produção");
            }

            validarFechamentoItem(item, itemRequest);
            item.registrarFechamento(itemRequest.quantidadeSobra(), itemRequest.acabouAntesDoFim(), itemRequest.horarioAcabou());
        }

        if (itensInformados.size() != itensPorId.size()) {
            throw new OperacaoInvalidaException("Todos os itens da produção devem ser informados no fechamento");
        }

        producao.fechar(request.clientesAtendidos(), request.restauranteAberto());
        return converterParaResponse(producaoRepository.save(producao));
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

    private void validarFechamentoItem(ItemProducao item, FechamentoItemRequest request) {
        if (request.quantidadeSobra().compareTo(item.getQuantidadeProduzida()) > 0) {
            throw new OperacaoInvalidaException("A sobra do item com ID " + item.getId() + " não pode superar a quantidade produzida");
        }

        if (request.acabouAntesDoFim() && request.horarioAcabou() == null) {
            throw new OperacaoInvalidaException("O horário é obrigatório quando o preparo acabou antes do fim");
        }

        if (request.acabouAntesDoFim() && request.quantidadeSobra().signum() > 0) {
            throw new OperacaoInvalidaException("Um preparo que acabou antes do fim não pode possuir sobra");
        }

        if (!request.acabouAntesDoFim() && request.horarioAcabou() != null) {
            throw new OperacaoInvalidaException("O horário só deve ser informado quando o preparo acabou antes do fim");
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
