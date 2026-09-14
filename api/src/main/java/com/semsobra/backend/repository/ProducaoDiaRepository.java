package com.semsobra.backend.repository;

import com.semsobra.backend.entity.ProducaoDia;
import com.semsobra.backend.entity.Turno;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ProducaoDiaRepository extends JpaRepository<ProducaoDia, Long> {

    boolean existsByDataAndTurno(LocalDate data, Turno turno);

    Optional<ProducaoDia> findByDataAndTurno(LocalDate data, Turno turno);

    List<ProducaoDia> findAllByOrderByDataDescTurnoAsc();
}
