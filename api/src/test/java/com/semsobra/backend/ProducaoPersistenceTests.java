package com.semsobra.backend;

import com.semsobra.backend.entity.ItemProducao;
import com.semsobra.backend.entity.Preparo;
import com.semsobra.backend.entity.ProducaoDia;
import com.semsobra.backend.entity.Turno;
import com.semsobra.backend.repository.ProducaoDiaRepository;
import com.semsobra.backend.repository.PreparoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@Transactional
class ProducaoPersistenceTests {

    @Autowired
    private PreparoRepository preparoRepository;

    @Autowired
    private ProducaoDiaRepository producaoDiaRepository;

    @Test
    void deveSalvarProducaoComItem() {
        Preparo preparo = preparoRepository.save(new Preparo("Preparo teste " + UUID.randomUUID(), "Teste", "kg"));
        ProducaoDia producao = new ProducaoDia(LocalDate.of(2099, 1, 5), Turno.ALMOCO);
        producao.adicionarItem(new ItemProducao(preparo, new BigDecimal("12.500")));

        ProducaoDia producaoSalva = producaoDiaRepository.saveAndFlush(producao);

        assertNotNull(producaoSalva.getId());
        assertEquals(1, producaoSalva.getItens().size());
        assertNotNull(producaoSalva.getItens().getFirst().getId());
        assertEquals(0, new BigDecimal("12.500").compareTo(producaoSalva.getItens().getFirst().getQuantidadeProduzida()));
    }
}
