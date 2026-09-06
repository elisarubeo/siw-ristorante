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
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/* Una voce dello scontrino: nome e prezzo sono copie, non riferimenti, cosi'
   restano quelli del momento della vendita anche se il piatto viene rinominato,
   ritoccato nel prezzo o tolto dal menu. */
@Entity
@Table(name = "riga_scontrino")
public class RigaScontrino {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scontrino_id", nullable = false)
    private Scontrino scontrino;

    @NotNull(message = "La quantità non può essere nulla")
    @Min(value = 1, message = "La quantità deve essere almeno 1")
    @Column(nullable = false)
    private Integer quantita;

    @NotBlank(message = "Il nome del piatto non può essere vuoto")
    @Size(max = 30, message = "Il nome non può superare i 30 caratteri")
    @Column(name = "nome_piatto", nullable = false, length = 30)
    private String nomePiatto;

    @NotNull(message = "Il prezzo unitario non può essere nullo")
    @DecimalMin(value = "0.00", message = "Il prezzo unitario non può essere negativo")
    @Column(name = "prezzo_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal prezzoUnitario;

    /* Id copiato, non associazione: permette di raggruppare per piatto nelle
       statistiche senza impedire che il piatto venga poi eliminato. */
    @NotNull(message = "L'identificativo del piatto non può essere nullo")
    @Column(name = "piatto_id", nullable = false)
    private Long piattoId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Scontrino getScontrino() {
        return scontrino;
    }

    public void setScontrino(Scontrino scontrino) {
        this.scontrino = scontrino;
    }

    public Integer getQuantita() {
        return quantita;
    }

    public void setQuantita(Integer quantita) {
        this.quantita = quantita;
    }

    public String getNomePiatto() {
        return nomePiatto;
    }

    public void setNomePiatto(String nomePiatto) {
        this.nomePiatto = nomePiatto;
    }

    public BigDecimal getPrezzoUnitario() {
        return prezzoUnitario;
    }

    public void setPrezzoUnitario(BigDecimal prezzoUnitario) {
        this.prezzoUnitario = prezzoUnitario;
    }

    public Long getPiattoId() {
        return piattoId;
    }

    public void setPiattoId(Long piattoId) {
        this.piattoId = piattoId;
    }

    /* Calcolato, non memorizzato: e' sempre prezzo per quantita'. */
    public BigDecimal getImporto() {
        if (prezzoUnitario == null || quantita == null) {
            return BigDecimal.ZERO;
        }
        return prezzoUnitario.multiply(BigDecimal.valueOf(quantita));
    }

    @Override
    public int hashCode() {
        return 31 + ((id == null) ? 0 : id.hashCode());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof RigaScontrino other))
            return false;
        return id != null && id.equals(other.getId());
    }
}
