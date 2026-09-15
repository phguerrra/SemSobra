package com.semsobra.backend;

import com.semsobra.backend.entity.ItemProducao;
import com.semsobra.backend.entity.Preparo;
import com.semsobra.backend.entity.ProducaoDia;
import com.semsobra.backend.entity.Turno;
import com.semsobra.backend.repository.PreparoRepository;
import com.semsobra.backend.repository.ProducaoDiaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RelatorioControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PreparoRepository preparoRepository;

    @Autowired
    private ProducaoDiaRepository producaoRepository;

    @Test
    void deveCalcularDesperdicioSomenteDeProducoesFechadas() throws Exception {
        Preparo arroz = criarPreparo("Arroz", "kg");
        Preparo feijao = criarPreparo("Feijão", "kg");

        criarProducaoFechada(LocalDate.of(2097, 2, 1), arroz, "10.000", "2.000", false);
        criarProducaoFechada(LocalDate.of(2097, 2, 2), arroz, "5.000", "0.000", true);
        criarProducaoFechada(LocalDate.of(2097, 2, 3), feijao, "20.000", "5.000", false);
        criarProducaoAberta(LocalDate.of(2097, 2, 4), feijao, "100.000");

        mockMvc.perform(get("/api/relatorios/desperdicio")
                        .param("dataInicio", "2097-02-01")
                        .param("dataFim", "2097-02-28"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].preparoId").value(feijao.getId()))
                .andExpect(jsonPath("$[0].quantidadeProduzida").value(20.0))
                .andExpect(jsonPath("$[0].quantidadeSobra").value(5.0))
                .andExpect(jsonPath("$[0].quantidadeConsumida").value(15.0))
                .andExpect(jsonPath("$[0].percentualDesperdicio").value(25.0))
                .andExpect(jsonPath("$[1].preparoId").value(arroz.getId()))
                .andExpect(jsonPath("$[1].quantidadeProduzida").value(15.0))
                .andExpect(jsonPath("$[1].quantidadeSobra").value(2.0))
                .andExpect(jsonPath("$[1].quantidadeConsumida").value(13.0))
                .andExpect(jsonPath("$[1].percentualDesperdicio").value(13.33))
                .andExpect(jsonPath("$[1].vezesAcabouAntesDoFim").value(1));
    }

    @Test
    void deveRejeitarPeriodoInvertido() throws Exception {
        mockMvc.perform(get("/api/relatorios/desperdicio")
                        .param("dataInicio", "2097-02-28")
                        .param("dataFim", "2097-02-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Operação inválida"));
    }

    private Preparo criarPreparo(String nome, String unidadeMedida) {
        return preparoRepository.save(new Preparo(nome + " relatório " + UUID.randomUUID(), "Teste", unidadeMedida));
    }

    private void criarProducaoFechada(LocalDate data, Preparo preparo, String produzida, String sobra, boolean acabouAntes) {
        ProducaoDia producao = new ProducaoDia(data, Turno.ALMOCO);
        ItemProducao item = new ItemProducao(preparo, new BigDecimal(produzida));

        item.registrarFechamento(new BigDecimal(sobra), acabouAntes, acabouAntes ? LocalTime.of(12, 30) : null);
        producao.adicionarItem(item);
        producao.fechar(100, true);
        producaoRepository.save(producao);
    }

    private void criarProducaoAberta(LocalDate data, Preparo preparo, String produzida) {
        ProducaoDia producao = new ProducaoDia(data, Turno.ALMOCO);

        producao.adicionarItem(new ItemProducao(preparo, new BigDecimal(produzida)));
        producaoRepository.save(producao);
    }
}
