package com.semsobra.backend.controller;

import com.semsobra.backend.dto.PreparoRequest;
import com.semsobra.backend.dto.PreparoResponse;
import com.semsobra.backend.service.PreparoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/preparos")
public class PreparoController {

    private final PreparoService service;

    public PreparoController(PreparoService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PreparoResponse criar(
            @Valid @RequestBody PreparoRequest request
    ) {
        return service.criar(request);
    }

    @GetMapping
    public List<PreparoResponse> listar(@RequestParam(name = "nome", required = false) String nome) {
        return service.listar(nome);
    }

    @GetMapping("/{id}")
    public PreparoResponse buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @PutMapping("/{id}")
    public PreparoResponse atualizar(
            @PathVariable Long id,
            @Valid @RequestBody PreparoRequest request
    ) {
        return service.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) {
        service.excluir(id);
    }
}