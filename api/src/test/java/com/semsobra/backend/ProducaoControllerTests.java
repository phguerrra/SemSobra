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
                .andExpect(jsonPath("$").isArray());

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
}
