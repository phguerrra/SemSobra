package com.semsobra.backend.repository;

import com.semsobra.backend.entity.Preparo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PreparoRepository extends JpaRepository<Preparo, Long> {

    boolean existsByNomeIgnoreCase(String nome);

    boolean existsByNomeIgnoreCaseAndIdNot(String nome, Long id);

    List<Preparo> findByNomeContainingIgnoreCaseOrderByNomeAsc(String nome);
}
