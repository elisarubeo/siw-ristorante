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

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotNull(message = "Il numero di persone non può essere nullo")
    private Integer numeroPersone;

    @Column(name = "data_prenotazione", nullable = false)
    private LocalDate dataPrenotazione;

    @Column(name = "orario_prenotazione", nullable = false)
    private LocalTime orarioPrenotazione;

    /* Senza una durata non si puo' dire fino a quando il tavolo e' occupato
       dalla prenotazione, e quindi nemmeno se e' libero a una certa ora. */
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

    /* Data e orario sono due colonne separate, ma per i confronti conviene
       ragionare su istanti: cosi' un turno che scavalca la mezzanotte non e'
       un caso particolare da trattare a parte. */
    public LocalDateTime getInizio() {
        return LocalDateTime.of(dataPrenotazione, orarioPrenotazione);
    }

    public LocalDateTime getFine() {
        return getInizio().plusMinutes(durataMinuti);
    }

    /* Vero se la prenotazione tiene impegnato il tavolo nell'istante dato.
       Estremo iniziale incluso, finale escluso: alle 22:00 in punto il tavolo
       di un turno 20:00-22:00 e' gia' di nuovo prenotabile. */
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
        if (!(obj instanceof Prenotazione other))
            return false;
        /* getId() e non other.id: su un proxy l'accesso diretto al campo
           restituisce null, il getter invece lo inizializza. */
        return id != null && id.equals(other.getId());
    }

    
}
