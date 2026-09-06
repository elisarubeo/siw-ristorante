package it.uniroma3.siw_ristorante.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/* Fotografia di un conto pagato. Si referenzia solo cio' che e' stabile (il
   ristorante) e si copiano i valori di cio' che e' volatile (numero del tavolo,
   nome e prezzo dei piatti): cosi' lo scontrino resta vero anche se il tavolo
   viene eliminato o il listino cambia. */
@Entity
public class Scontrino {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    /* Il momento del pagamento, non quello di apertura del conto. Senza questo
       campo non si possono fare incassi per periodo ne' statistiche. */
    @NotNull(message = "La data dello scontrino non può essere nulla")
    @Column(name = "data_ora", nullable = false)
    private LocalDateTime dataOra;

    /* Numero copiato, non associazione: conserva l'informazione senza creare un
       vincolo che impedirebbe di eliminare il tavolo. */
    @Column(name = "numero_tavolo")
    private Integer numeroTavolo;

    /* BigDecimal e non double: sui soldi la virgola mobile accumula errori e il
       totale puo' non coincidere con la somma delle righe. */
    @NotNull(message = "Il totale non può essere nullo")
    @DecimalMin(value = "0.00", message = "Il totale non può essere negativo")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totale;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ristorante_id", nullable = false)
    private Ristorante ristorante;

    @OneToMany(mappedBy = "scontrino", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RigaScontrino> righe = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getDataOra() {
        return dataOra;
    }

    public void setDataOra(LocalDateTime dataOra) {
        this.dataOra = dataOra;
    }

    public Integer getNumeroTavolo() {
        return numeroTavolo;
    }

    public void setNumeroTavolo(Integer numeroTavolo) {
        this.numeroTavolo = numeroTavolo;
    }

    public BigDecimal getTotale() {
        return totale;
    }

    public void setTotale(BigDecimal totale) {
        this.totale = totale;
    }

    public Ristorante getRistorante() {
        return ristorante;
    }

    public void setRistorante(Ristorante ristorante) {
        this.ristorante = ristorante;
    }

    public List<RigaScontrino> getRighe() {
        return righe;
    }

    public void setRighe(List<RigaScontrino> righe) {
        this.righe = righe;
    }

    /* Aggiorna entrambi i lati dell'associazione: usare questo invece di
       getRighe().add(...) quando si costruisce lo scontrino alla chiusura. */
    public void aggiungiRiga(RigaScontrino riga) {
        righe.add(riga);
        riga.setScontrino(this);
    }

    public void rimuoviRiga(RigaScontrino riga) {
        righe.remove(riga);
        riga.setScontrino(null);
    }

    /* Somma delle righe, utile per verificare che il totale memorizzato torni. */
    public BigDecimal calcolaTotaleRighe() {
        return righe.stream()
                .map(RigaScontrino::getImporto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
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
           runtime. */
        if (!(obj instanceof Scontrino other))
            return false;
        return id != null && id.equals(other.getId());
    }
}
