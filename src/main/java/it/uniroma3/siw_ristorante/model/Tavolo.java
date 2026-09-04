package it.uniroma3.siw_ristorante.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotNull;

@Entity
public class Tavolo {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotNull(message = "Il numero di posti del tavolo non può essere nullo")
    @Column(nullable = false)
    private Integer numeroPosti;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ristorante_id", nullable = false)
    private Ristorante ristorante;

    @OneToMany(mappedBy = "tavolo")
    private List<Ordinazione> ordinazioni = new ArrayList<>();

    @OneToMany(mappedBy = "tavolo")
    private List<Prenotazione> prenotazioni = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
