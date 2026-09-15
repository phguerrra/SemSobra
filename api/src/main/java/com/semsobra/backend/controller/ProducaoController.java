package com.semsobra.backend.controller;

import com.semsobra.backend.dto.FechamentoProducaoRequest;
import com.semsobra.backend.dto.ProducaoRequest;
import com.semsobra.backend.dto.ProducaoResponse;
import com.semsobra.backend.service.ProducaoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/producoes")
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
    public List<ProducaoResponse> listar() {
        return service.listar();
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
