package model;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;

@Entity
public class Prenotazione {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotNull(message = "Il numero di persone non può essere nullo")
    private Integer numeroPersone;

    @Column(name = "data_prenotazione", nullable = false)
    private LocalDate dataPrenotazione;

    @Column(name = "orario_prenotazione", nullable = false)
    private LocalTime orarioPrenotazione;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private StatoPrenotazione status = StatoPrenotazione.SCHEDULED;

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

    public StatoPrenotazione getStatus() {
        return status;
    }

    public void setStatus(StatoPrenotazione status) {
        this.status = status;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Prenotazione other = (Prenotazione) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }

    
}
