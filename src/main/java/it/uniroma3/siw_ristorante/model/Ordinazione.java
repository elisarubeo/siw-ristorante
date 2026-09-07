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

@Entity
public class Ordinazione {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotNull(message = "Il totale dell'ordinazione non può essere nullo")
    @DecimalMin(value = "0.00", message = "Il totale non può essere negativo")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totale;

    /* Un'ordinazione ancora aperta e' il fatto che dice "a questo tavolo c'e'
       gente seduta". La chiusura nulla significa aperta: non serve un enum di
       stato, e cosi' si puo' sapere se il tavolo era occupato anche a un
       istante passato. */
    @NotNull(message = "L'orario di apertura non puo' essere nullo")
    @Column(nullable = false)
    private LocalDateTime apertura = LocalDateTime.now();

    @Column
    private LocalDateTime chiusura;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tavolo_id", nullable = false)
    private Tavolo tavolo;

    @OneToMany(mappedBy = "ordinazione", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RigaOrdinazione> righe = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BigDecimal getTotale() {
        return totale;
    }

    public void setTotale(BigDecimal totale) {
        this.totale = totale;
    }

    public LocalDateTime getApertura() {
        return apertura;
    }

    public void setApertura(LocalDateTime apertura) {
        this.apertura = apertura;
    }

    public LocalDateTime getChiusura() {
        return chiusura;
    }

    public void setChiusura(LocalDateTime chiusura) {
        this.chiusura = chiusura;
    }

    public boolean isAperta(LocalDateTime istante) {
        return !istante.isBefore(apertura) && (chiusura == null || istante.isBefore(chiusura));
    }

    public Tavolo getTavolo() {
        return tavolo;
    }

    public void setTavolo(Tavolo tavolo) {
        this.tavolo = tavolo;
    }

    public List<RigaOrdinazione> getRighe() {
        return righe;
    }

    public void setRighe(List<RigaOrdinazione> righe) {
        this.righe = righe;
    }

    @Override
    public int hashCode() {
        return 31 + ((id == null) ? 0 : id.hashCode());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof Ordinazione other))
            return false;
        return id != null && id.equals(other.getId());
    }

    public void aggiungiRiga(RigaOrdinazione riga) {
        righe.add(riga);
        riga.setOrdinazione(this);
    }

    public void rimuoviRiga(RigaOrdinazione riga) {
        righe.remove(riga);
        riga.setOrdinazione(null);
    }

    public BigDecimal calcolaTotaleRighe() {
        return righe.stream()
                .map(RigaOrdinazione::getImporto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
}
