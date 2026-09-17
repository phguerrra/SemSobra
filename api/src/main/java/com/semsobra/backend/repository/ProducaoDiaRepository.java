package com.semsobra.backend.repository;

import com.semsobra.backend.entity.ProducaoDia;
import com.semsobra.backend.entity.Turno;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ProducaoDiaRepository extends JpaRepository<ProducaoDia, Long>, JpaSpecificationExecutor<ProducaoDia> {

    boolean existsByDataAndTurno(LocalDate data, Turno turno);

    Optional<ProducaoDia> findByDataAndTurno(LocalDate data, Turno turno);

    @EntityGraph(attributePaths = {"itens", "itens.preparo"})
    List<ProducaoDia> findAllByFechadoTrueAndDataBetweenOrderByDataAsc(LocalDate dataInicio, LocalDate dataFim);
}
