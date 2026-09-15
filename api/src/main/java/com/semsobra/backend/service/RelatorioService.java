package com.semsobra.backend.service;

import com.semsobra.backend.dto.RelatorioDesperdicioResponse;
import com.semsobra.backend.entity.ItemProducao;
import com.semsobra.backend.entity.Preparo;
import com.semsobra.backend.exception.OperacaoInvalidaException;
import com.semsobra.backend.repository.ProducaoDiaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RelatorioService {

    private final ProducaoDiaRepository producaoRepository;

    public RelatorioService(ProducaoDiaRepository producaoRepository) {
        this.producaoRepository = producaoRepository;
    }

    @Transactional(readOnly = true)
    public List<RelatorioDesperdicioResponse> calcularDesperdicio(LocalDate dataInicio, LocalDate dataFim) {
        if (dataInicio.isAfter(dataFim)) {
            throw new OperacaoInvalidaException("A data inicial não pode ser posterior à data final");
        }

        Map<Long, AcumuladorDesperdicio> totaisPorPreparo = new LinkedHashMap<>();

        producaoRepository.findAllByFechadoTrueAndDataBetweenOrderByDataAsc(dataInicio, dataFim).forEach(
                producao -> producao.getItens().forEach(item -> totaisPorPreparo
                        .computeIfAbsent(item.getPreparo().getId(), id -> new AcumuladorDesperdicio(item.getPreparo()))
                        .adicionar(item))
        );

        return totaisPorPreparo.values().stream()
                .map(AcumuladorDesperdicio::converterParaResponse)
                .sorted(Comparator.comparing(RelatorioDesperdicioResponse::percentualDesperdicio).reversed()
                        .thenComparing(RelatorioDesperdicioResponse::preparoNome, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private static class AcumuladorDesperdicio {

        private final Preparo preparo;
        private BigDecimal quantidadeProduzida = BigDecimal.ZERO;
        private BigDecimal quantidadeSobra = BigDecimal.ZERO;
        private long vezesAcabouAntesDoFim;

        private AcumuladorDesperdicio(Preparo preparo) {
            this.preparo = preparo;
        }

        private void adicionar(ItemProducao item) {
            quantidadeProduzida = quantidadeProduzida.add(item.getQuantidadeProduzida());
            quantidadeSobra = quantidadeSobra.add(item.getQuantidadeSobra());

            if (item.isAcabouAntesDoFim()) {
                vezesAcabouAntesDoFim++;
            }
        }

        private RelatorioDesperdicioResponse converterParaResponse() {
            BigDecimal quantidadeConsumida = quantidadeProduzida.subtract(quantidadeSobra);
            BigDecimal percentual = quantidadeProduzida.signum() == 0
                    ? BigDecimal.ZERO.setScale(2)
                    : quantidadeSobra.multiply(BigDecimal.valueOf(100)).divide(quantidadeProduzida, 2, RoundingMode.HALF_UP);

            return new RelatorioDesperdicioResponse(
                    preparo.getId(),
                    preparo.getNome(),
                    preparo.getUnidadeMedida(),
                    quantidadeProduzida,
                    quantidadeSobra,
                    quantidadeConsumida,
                    percentual,
                    vezesAcabouAntesDoFim
            );
        }
    }
}
