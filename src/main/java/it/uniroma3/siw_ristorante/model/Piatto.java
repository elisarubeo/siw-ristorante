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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
public class Piatto {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotBlank(message = "Il nome del piatto non può essere vuoto")
    @Size(max = 30, message = "Il nome non può superare i 30 caratteri")
    @Column(nullable = false, length = 30)
    private String nome;

    @NotBlank(message = "Gli ingredienti del piatto non possono essere vuoti")
    @Size(max = 100, message = "Gli ingredienti non possono superare i 100 caratteri")
    @Column(nullable = false, length = 100)
    private String ingredienti;

    @NotNull(message = "Il prezzo del piatto non può essere nullo")
    @DecimalMin(value = "0.01", message = "Il prezzo deve essere maggiore di zero")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal prezzo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ristorante_id", nullable = false)
    private Ristorante ristorante;

    @Column(nullable = false)
    private boolean disponibile = true;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getIngredienti() {
        return ingredienti;
    }

    public void setIngredienti(String ingredienti) {
        this.ingredienti = ingredienti;
    }

    public BigDecimal getPrezzo() {
        return prezzo;
    }

    public void setPrezzo(BigDecimal prezzo) {
        this.prezzo = prezzo;
    }

    public Ristorante getRistorante() {
        return ristorante;
    }

    public void setRistorante(Ristorante ristorante) {
        this.ristorante = ristorante;
    }

    @Override
    public int hashCode() {
        return 31 + ((id == null) ? 0 : id.hashCode());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof Piatto other))
            return false;
        return id != null && id.equals(other.getId());
    }

    public boolean isDisponibile() {
        return disponibile;
    }

    public void setDisponibile(boolean disponibile) {
        this.disponibile = disponibile;
    }

    
}
