package com.semsobra.backend;

import com.semsobra.backend.entity.Preparo;
import com.semsobra.backend.entity.Turno;
import com.semsobra.backend.repository.PreparoRepository;
import com.semsobra.backend.repository.ProducaoDiaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProducaoControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PreparoRepository preparoRepository;

    @Autowired
    private ProducaoDiaRepository producaoDiaRepository;

    @Test
    void deveCriarListarEBuscarProducao() throws Exception {
        Preparo preparo = criarPreparo();
        String corpo = criarCorpo(preparo.getId(), "2098-01-10", "12.500");

        mockMvc.perform(post("/api/producoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data").value("2098-01-10"))
                .andExpect(jsonPath("$.turno").value("ALMOCO"))
                .andExpect(jsonPath("$.itens[0].preparoId").value(preparo.getId()))
                .andExpect(jsonPath("$.itens[0].preparoNome").value(preparo.getNome()))
                .andExpect(jsonPath("$.itens[0].quantidadeProduzida").value(12.5))
                .andReturn();

        Long id = producaoDiaRepository.findByDataAndTurno(LocalDate.of(2098, 1, 10), Turno.ALMOCO).orElseThrow().getId();

        mockMvc.perform(get("/api/producoes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo").isArray());

        mockMvc.perform(get("/api/producoes/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.itens[0].quantidadeSobra").value(0));
    }

    @Test
    void deveRejeitarProducaoDuplicada() throws Exception {
        Preparo preparo = criarPreparo();
        String corpo = criarCorpo(preparo.getId(), "2098-01-11", "8.000");

        mockMvc.perform(post("/api/producoes").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/producoes").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Recurso duplicado"));
    }

    @Test
    void deveRejeitarQuantidadeInvalida() throws Exception {
        Preparo preparo = criarPreparo();
        String corpo = criarCorpo(preparo.getId(), "2098-01-12", "0");

        mockMvc.perform(post("/api/producoes").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Erro de validação"));
    }

    @Test
    void deveRetornarNaoEncontradoParaPreparoInexistente() throws Exception {
        String corpo = criarCorpo(Long.MAX_VALUE, "2098-01-13", "5.000");

        mockMvc.perform(post("/api/producoes").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso não encontrado"));
    }

    @Test
    void deveRejeitarPreparoRepetidoNaProducao() throws Exception {
        Preparo preparo = criarPreparo();
        String corpo = """
                {
                  "data": "2098-01-14",
                  "turno": "ALMOCO",
                  "itens": [
                    {"preparoId": %d, "quantidadeProduzida": 5.000},
                    {"preparoId": %d, "quantidadeProduzida": 3.000}
                  ]
                }
                """.formatted(preparo.getId(), preparo.getId());

        mockMvc.perform(post("/api/producoes").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Recurso duplicado"));
    }

    @Test
    void deveFecharProducao() throws Exception {
        Preparo preparo = criarPreparo();
        Long producaoId = criarProducao(preparo.getId(), "2098-01-15", "12.500");
        Long itemId = producaoDiaRepository.findById(producaoId).orElseThrow().getItens().getFirst().getId();
        String fechamento = criarFechamento(itemId, "2.500", false, null);

        mockMvc.perform(patch("/api/producoes/{id}/fechamento", producaoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(fechamento))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fechado").value(true))
                .andExpect(jsonPath("$.clientesAtendidos").value(120))
                .andExpect(jsonPath("$.itens[0].quantidadeSobra").value(2.5));

        mockMvc.perform(get("/api/producoes/{id}", producaoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fechado").value(true));
    }

    @Test
    void deveRejeitarSegundoFechamento() throws Exception {
        Preparo preparo = criarPreparo();
        Long producaoId = criarProducao(preparo.getId(), "2098-01-16", "10.000");
        Long itemId = producaoDiaRepository.findById(producaoId).orElseThrow().getItens().getFirst().getId();
        String fechamento = criarFechamento(itemId, "1.000", false, null);

        mockMvc.perform(patch("/api/producoes/{id}/fechamento", producaoId).contentType(MediaType.APPLICATION_JSON).content(fechamento))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/producoes/{id}/fechamento", producaoId).contentType(MediaType.APPLICATION_JSON).content(fechamento))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Recurso duplicado"));
    }

    @Test
    void deveRejeitarSobraMaiorQueQuantidadeProduzida() throws Exception {
        Preparo preparo = criarPreparo();
        Long producaoId = criarProducao(preparo.getId(), "2098-01-17", "5.000");
        Long itemId = producaoDiaRepository.findById(producaoId).orElseThrow().getItens().getFirst().getId();

        mockMvc.perform(patch("/api/producoes/{id}/fechamento", producaoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(criarFechamento(itemId, "6.000", false, null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Operação inválida"));
    }

    @Test
    void deveExigirHorarioQuandoPreparoAcabouAntesDoFim() throws Exception {
        Preparo preparo = criarPreparo();
        Long producaoId = criarProducao(preparo.getId(), "2098-01-18", "5.000");
        Long itemId = producaoDiaRepository.findById(producaoId).orElseThrow().getItens().getFirst().getId();

        mockMvc.perform(patch("/api/producoes/{id}/fechamento", producaoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(criarFechamento(itemId, "0", true, null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Operação inválida"));
    }

    @Test
    void deveFiltrarHistoricoPorPeriodoTurnoESituacao() throws Exception {
        Preparo preparo = criarPreparo();
        criarProducao(preparo.getId(), "2098-03-10", "5.000");
        Long producaoEsperada = criarProducao(preparo.getId(), "2098-03-11", "6.000");

        mockMvc.perform(get("/api/producoes")
                        .param("dataInicio", "2098-03-11")
                        .param("dataFim", "2098-03-11")
                        .param("turno", "ALMOCO")
                        .param("fechado", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.conteudo[0].id").value(producaoEsperada))
                .andExpect(jsonPath("$.conteudo[0].data").value("2098-03-11"));
    }

    @Test
    void devePaginarHistoricoDoMaisRecenteParaOMaisAntigo() throws Exception {
        Preparo preparo = criarPreparo();
        criarProducao(preparo.getId(), "2098-03-12", "5.000");
        Long producaoMaisRecente = criarProducao(preparo.getId(), "2098-03-13", "6.000");

        mockMvc.perform(get("/api/producoes").param("pagina", "0").param("tamanho", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.tamanho").value(1))
                .andExpect(jsonPath("$.totalElementos").value(2))
                .andExpect(jsonPath("$.totalPaginas").value(2))
                .andExpect(jsonPath("$.primeira").value(true))
                .andExpect(jsonPath("$.ultima").value(false))
                .andExpect(jsonPath("$.conteudo[0].id").value(producaoMaisRecente));
    }

    @Test
    void deveRejeitarPeriodoInvertidoNoHistorico() throws Exception {
        mockMvc.perform(get("/api/producoes")
                        .param("dataInicio", "2098-03-31")
                        .param("dataFim", "2098-03-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Operação inválida"));
    }

    @Test
    void deveLimitarQuantidadeDeRegistrosPorPagina() throws Exception {
        mockMvc.perform(get("/api/producoes").param("tamanho", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Erro de validação"));
    }

    private Preparo criarPreparo() {
        return preparoRepository.save(new Preparo("Preparo API " + UUID.randomUUID(), "Teste", "kg"));
    }

    private String criarCorpo(Long preparoId, String data, String quantidade) {
        return """
                {
                  "data": "%s",
                  "turno": "ALMOCO",
                  "itens": [
                    {
                      "preparoId": %d,
                      "quantidadeProduzida": %s
                    }
                  ]
                }
                """.formatted(data, preparoId, quantidade);
    }

    private Long criarProducao(Long preparoId, String data, String quantidade) throws Exception {
        mockMvc.perform(post("/api/producoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(criarCorpo(preparoId, data, quantidade)))
                .andExpect(status().isCreated());

        return producaoDiaRepository.findByDataAndTurno(LocalDate.parse(data), Turno.ALMOCO).orElseThrow().getId();
    }

    private String criarFechamento(Long itemId, String quantidadeSobra, boolean acabouAntesDoFim, String horarioAcabou) {
        String horario = horarioAcabou == null ? "null" : "\"" + horarioAcabou + "\"";

        return """
                {
                  "clientesAtendidos": 120,
                  "restauranteAberto": true,
                  "itens": [
                    {
                      "itemId": %d,
                      "quantidadeSobra": %s,
                      "acabouAntesDoFim": %s,
                      "horarioAcabou": %s
                    }
                  ]
                }
                """.formatted(itemId, quantidadeSobra, acabouAntesDoFim, horario);
    }
}
