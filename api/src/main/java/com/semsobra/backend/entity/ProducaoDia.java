package com.semsobra.backend.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(
        name = "producoes_diarias",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_producoes_diarias_data_turno",
                        columnNames = {"data", "turno"}
                )
        }
)
public class ProducaoDia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate data;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Turno turno;

    @Column(name = "clientes_atendidos", nullable = false)
    private int clientesAtendidos;

    @Column(name = "restaurante_aberto", nullable = false)
    private boolean restauranteAberto = true;

    @Column(nullable = false)
    private boolean fechado;

    @OneToMany(mappedBy = "producaoDia", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<ItemProducao> itens = new ArrayList<>();

    protected ProducaoDia() {
    }

    public ProducaoDia(LocalDate data, Turno turno) {
        this.data = data;
        this.turno = turno;
    }

    public Long getId() {
        return id;
    }

    public LocalDate getData() {
        return data;
    }

    public Turno getTurno() {
        return turno;
    }

    public int getClientesAtendidos() {
        return clientesAtendidos;
    }

    public boolean isRestauranteAberto() {
        return restauranteAberto;
    }

    public boolean isFechado() {
        return fechado;
    }

    public List<ItemProducao> getItens() {
        return Collections.unmodifiableList(itens);
    }

    public void fechar(int clientesAtendidos, boolean restauranteAberto) {
        this.clientesAtendidos = clientesAtendidos;
        this.restauranteAberto = restauranteAberto;
        this.fechado = true;
    }

    public void adicionarItem(ItemProducao item) {
        item.definirProducaoDia(this);
        itens.add(item);
    }
}
