package com.semsobra.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.LocalTime;

@Entity
@Table(
        name = "itens_producao",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_itens_producao_dia_preparo",
                        columnNames = {"producao_dia_id", "preparo_id"}
                )
        }
)
public class ItemProducao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producao_dia_id", nullable = false)
    private ProducaoDia producaoDia;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "preparo_id", nullable = false)
    private Preparo preparo;

    @Column(name = "quantidade_produzida", nullable = false, precision = 12, scale = 3)
    private BigDecimal quantidadeProduzida;

    @Column(name = "quantidade_sobra", nullable = false, precision = 12, scale = 3)
    private BigDecimal quantidadeSobra = BigDecimal.ZERO;

    @Column(name = "acabou_antes_do_fim", nullable = false)
    private boolean acabouAntesDoFim;

    @Column(name = "horario_acabou")
    private LocalTime horarioAcabou;

    protected ItemProducao() {
    }

    public ItemProducao(Preparo preparo, BigDecimal quantidadeProduzida) {
        this.preparo = preparo;
        this.quantidadeProduzida = quantidadeProduzida;
    }

    public Long getId() {
        return id;
    }

    public ProducaoDia getProducaoDia() {
        return producaoDia;
    }

    public Preparo getPreparo() {
        return preparo;
    }

    public BigDecimal getQuantidadeProduzida() {
        return quantidadeProduzida;
    }

    public BigDecimal getQuantidadeSobra() {
        return quantidadeSobra;
    }

    public boolean isAcabouAntesDoFim() {
        return acabouAntesDoFim;
    }

    public LocalTime getHorarioAcabou() {
        return horarioAcabou;
    }

    public void setQuantidadeSobra(BigDecimal quantidadeSobra) {
        this.quantidadeSobra = quantidadeSobra;
    }

    public void setAcabouAntesDoFim(boolean acabouAntesDoFim) {
        this.acabouAntesDoFim = acabouAntesDoFim;
    }

    public void setHorarioAcabou(LocalTime horarioAcabou) {
        this.horarioAcabou = horarioAcabou;
    }

    void definirProducaoDia(ProducaoDia producaoDia) {
        this.producaoDia = producaoDia;
    }
}
