package com.semsobra.backend.controller;

import com.semsobra.backend.dto.FechamentoProducaoRequest;
import com.semsobra.backend.dto.PaginaResponse;
import com.semsobra.backend.dto.ProducaoRequest;
import com.semsobra.backend.dto.ProducaoResponse;
import com.semsobra.backend.entity.Turno;
import com.semsobra.backend.service.ProducaoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/producoes")
@Validated
public class ProducaoController {

    private final ProducaoService service;

    public ProducaoController(ProducaoService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProducaoResponse criar(@Valid @RequestBody ProducaoRequest request) {
        return service.criar(request);
    }

    @GetMapping
    public PaginaResponse<ProducaoResponse> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(required = false) Turno turno,
            @RequestParam(required = false) Boolean fechado,
            @RequestParam(defaultValue = "0") @Min(0) int pagina,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamanho
    ) {
        return service.listar(dataInicio, dataFim, turno, fechado, pagina, tamanho);
    }

    @GetMapping("/{id}")
    public ProducaoResponse buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @PatchMapping("/{id}/fechamento")
    public ProducaoResponse fechar(@PathVariable Long id, @Valid @RequestBody FechamentoProducaoRequest request) {
        return service.fechar(id, request);
    }
}
