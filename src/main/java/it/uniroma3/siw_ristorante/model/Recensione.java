package it.uniroma3.siw_ristorante.model;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/* Un utente puo' recensire un ristorante una volta sola. Il controllo c'e'
   gia' in RecensioneService.save, ma e' una lettura seguita da una scrittura:
   due richieste contemporanee dello stesso utente le passano tutte e due.
   Il vincolo sul database e' l'unico punto in cui la regola non si aggira. */
@Entity
@Table(name = "recensione",
        uniqueConstraints = @UniqueConstraint(columnNames = { "user_id", "ristorante_id" }))
public class Recensione {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotBlank(message = "Il titolo della recensione non può essere vuoto")
    @Size(max = 30, message = "Il titolo non può superare i 30 caratteri")
    @Column(nullable = false, length = 30)
    private String titolo;

    @NotNull(message = "Il voto della recensione non può essere nullo")
    @Min(value = 1, message = "Il voto minimo è 1")
    @Max(value = 5, message = "Il voto massimo è 5")
    @Column(nullable = false)
    private Integer voto;

    @Size(max = 200, message = "Il commento non può superare i 200 caratteri")
    @Column(length = 200)
    private String testo;

    @Column(nullable = false)
    private LocalDate data;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ristorante_id", nullable = false)
    private Ristorante ristorante;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitolo() {
        return titolo;
    }

    public void setTitolo(String titolo) {
        this.titolo = titolo;
    }

    public Integer getVoto() {
        return voto;
    }

    public void setVoto(Integer voto) {
        this.voto = voto;
    }

    public String getTesto() {
        return testo;
    }

    public void setTesto(String testo) {
        this.testo = testo;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
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
        if (!(obj instanceof Recensione other))
            return false;
        return id != null && id.equals(other.getId());
    }

    
}
