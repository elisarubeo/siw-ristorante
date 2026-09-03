package it.uniroma3.siw_ristorante.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;

@Entity
public class Tavolo {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotNull(message = "Il numero di posti del tavolo non può essere nullo")
    @Column(nullable = false)
    private Integer numeroPosti;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private StatoTavolo status = StatoTavolo.FREE;

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

    public StatoTavolo getStatus() {
        return status;
    }

    public void setStatus(StatoTavolo status) {
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
        Tavolo other = (Tavolo) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }

    
}
