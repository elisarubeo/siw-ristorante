package it.uniroma3.siw_ristorante.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/* Il numero di tavolo e' unico dentro il singolo ristorante, non nel database:
   ogni locale ha il suo tavolo 1. */
@Entity
@Table(name = "tavolo",
        uniqueConstraints = @UniqueConstraint(columnNames = { "ristorante_id", "numero_tavolo" }))
public class Tavolo {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotNull(message = "Il numero del tavolo non può essere nullo")
    @Min(value = 1, message = "Il numero del tavolo deve essere almeno 1")
    @Column(name = "numero_tavolo", nullable = false)
    private Integer numeroTavolo;

    @NotNull(message = "Il numero di posti del tavolo non può essere nullo")
    @Min(value = 1, message = "Il tavolo deve avere almeno un posto")
    @Max(value = 20, message = "Il tavolo non può avere più di 20 posti")
    @Column(nullable = false)
    private Integer numeroPosti;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private StatoTavolo status = StatoTavolo.LIBERO;

    public StatoTavolo getStatus() {
        return status;
    }

    public void setStatus(StatoTavolo status) {
        this.status = status;
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ristorante_id", nullable = false)
    private Ristorante ristorante;

    @OneToMany(mappedBy = "tavolo")
    private List<Ordinazione> ordinazioni = new ArrayList<>();

    @OneToMany(mappedBy = "tavolo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Prenotazione> prenotazioni = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getNumeroTavolo() {
        return numeroTavolo;
    }

    public void setNumeroTavolo(Integer numeroTavolo) {
        this.numeroTavolo = numeroTavolo;
    }

    public Integer getNumeroPosti() {
        return numeroPosti;
    }

    public void setNumeroPosti(Integer numeroPosti) {
        this.numeroPosti = numeroPosti;
    }

    public Ristorante getRistorante() {
        return ristorante;
    }

    public void setRistorante(Ristorante ristorante) {
        this.ristorante = ristorante;
    }

    public List<Ordinazione> getOrdinazioni() {
        return ordinazioni;
    }

    public void setOrdinazioni(List<Ordinazione> ordinazioni) {
        this.ordinazioni = ordinazioni;
    }

    public List<Prenotazione> getPrenotazioni() {
        return prenotazioni;
    }

    public void setPrenotazioni(List<Prenotazione> prenotazioni) {
        this.prenotazioni = prenotazioni;
    }

    @Override
    public int hashCode() {
        return 31 + ((id == null) ? 0 : id.hashCode());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        /* instanceof e non getClass(): con le associazioni LAZY Hibernate
           consegna dei proxy, la cui classe e' una sottoclasse generata a
           runtime. getClass() != obj.getClass() farebbe risultare diversi un
           proxy e l'entita' che rappresenta. */
        if (!(obj instanceof Tavolo other))
            return false;
        /* getId() e non other.id: su un proxy l'accesso diretto al campo
           restituisce null, il getter invece lo inizializza. */
        return id != null && id.equals(other.getId());
    }

    
}
