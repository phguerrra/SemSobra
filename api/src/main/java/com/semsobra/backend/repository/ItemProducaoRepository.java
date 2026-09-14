package com.semsobra.backend.repository;

import com.semsobra.backend.entity.ItemProducao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ItemProducaoRepository extends JpaRepository<ItemProducao, Long> {

    List<ItemProducao> findByProducaoDiaIdOrderByIdAsc(Long producaoDiaId);
}
