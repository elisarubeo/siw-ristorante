package it.uniroma3.siw_ristorante.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

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
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Entity
public class Prenotazione {

    /* Durata del turno a tavola, se non specificata altrimenti. */
    public static final int DURATA_PREDEFINITA_MINUTI = 120;

    /* Quanto in anticipo si puo' prenotare. A differenza della durata questa
       non si memorizza nella riga: e' una regola che vale al momento in cui si
       prenota, non una caratteristica del turno prenotato. */
    public static final int ANTICIPO_MASSIMO_MESI = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotNull(message = "Il numero di persone non può essere nullo")
    @Min(value = 1, message = "Serve almeno una persona")
    private Integer numeroPersone;

    @NotNull(message = "La data non può essere vuota")
    @Column(name = "data_prenotazione", nullable = false)
    private LocalDate dataPrenotazione;

    @NotNull(message = "L'orario non può essere vuoto")
    @Column(name = "orario_prenotazione", nullable = false)
    private LocalTime orarioPrenotazione;

    @NotNull(message = "La durata della prenotazione non puo' essere nulla")
    @Min(value = 1, message = "La durata deve essere di almeno un minuto")
    @Column(name = "durata_minuti", nullable = false)
    private Integer durataMinuti = DURATA_PREDEFINITA_MINUTI;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private StatoPrenotazione status = StatoPrenotazione.SCHEDULED;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ristorante_id", nullable = false)
    private Ristorante ristorante;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tavolo_id", nullable = false)
    private Tavolo tavolo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getNumeroPersone() {
        return numeroPersone;
    }

    public void setNumeroPersone(Integer numeroPersone) {
        this.numeroPersone = numeroPersone;
    }

    public LocalDate getDataPrenotazione() {
        return dataPrenotazione;
    }

    public void setDataPrenotazione(LocalDate dataPrenotazione) {
        this.dataPrenotazione = dataPrenotazione;
    }

    public LocalTime getOrarioPrenotazione() {
        return orarioPrenotazione;
    }

    public void setOrarioPrenotazione(LocalTime orarioPrenotazione) {
        this.orarioPrenotazione = orarioPrenotazione;
    }

    public Integer getDurataMinuti() {
        return durataMinuti;
    }

    public void setDurataMinuti(Integer durataMinuti) {
        this.durataMinuti = durataMinuti;
    }

    public LocalDateTime getInizio() {
        return LocalDateTime.of(dataPrenotazione, orarioPrenotazione);
    }

    public LocalDateTime getFine() {
        return getInizio().plusMinutes(durataMinuti);
    }

    // Due intervalli si sovrappongono se ciascuno comincia prima che l'altro finisca.
    public boolean sovrappone(LocalDateTime altroInizio, LocalDateTime altraFine) {
        return getInizio().isBefore(altraFine) && altroInizio.isBefore(getFine());
    }

    public boolean copre(LocalDateTime istante) {
        return !istante.isBefore(getInizio()) && istante.isBefore(getFine());
    }

    public StatoPrenotazione getStatus() {
        return status;
    }

    public void setStatus(StatoPrenotazione status) {
        this.status = status;
    }

    public Ristorante getRistorante() {
        return ristorante;
    }

    public void setRistorante(Ristorante ristorante) {
        this.ristorante = ristorante;
    }

    public Tavolo getTavolo() {
        return tavolo;
    }

    public void setTavolo(Tavolo tavolo) {
        this.tavolo = tavolo;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @Override
    public int hashCode() {
        return 31 + ((id == null) ? 0 : id.hashCode());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof Prenotazione other))
            return false;
        return id != null && id.equals(other.getId());
    }

    
}
