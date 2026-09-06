package it.uniroma3.siw_ristorante.model;

import java.math.BigDecimal;

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
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/* Entita' associativa fra Ordinazione e Piatto: rispetto a un @ManyToMany
   diretto puo' portare la quantita'. */
@Entity
@Table(name = "riga_ordinazione",
        uniqueConstraints = @UniqueConstraint(columnNames = { "ordinazione_id", "piatto_id" }))
public class RigaOrdinazione {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ordinazione_id", nullable = false)
    private Ordinazione ordinazione;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "piatto_id", nullable = false)
    private Piatto piatto;

    @NotNull(message = "La quantita' non puo' essere nulla")
    @Min(value = 1, message = "La quantita' deve essere almeno 1")
    @Column(nullable = false)
    private Integer quantita;

    @NotNull(message = "Il prezzo unitario non può essere nullo")
    @DecimalMin(value = "0.00", message = "Il prezzo unitario non può essere negativo")
    @Column(name = "prezzo_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal prezzoUnitario;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Ordinazione getOrdinazione() {
        return ordinazione;
    }

    public void setOrdinazione(Ordinazione ordinazione) {
        this.ordinazione = ordinazione;
    }

    public Piatto getPiatto() {
        return piatto;
    }

    public void setPiatto(Piatto piatto) {
        this.piatto = piatto;
    }

    public Integer getQuantita() {
        return quantita;
    }

    public void setQuantita(Integer quantita) {
        this.quantita = quantita;
    }

    @Override
    public int hashCode() {
        return 31 + ((id == null) ? 0 : id.hashCode());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof RigaOrdinazione other))
            return false;
        return id != null && id.equals(other.getId());
    }

    public BigDecimal getPrezzoUnitario() {
        return prezzoUnitario;
    }

    public void setPrezzoUnitario(BigDecimal prezzoUnitario) {
        this.prezzoUnitario = prezzoUnitario;
    }
}
