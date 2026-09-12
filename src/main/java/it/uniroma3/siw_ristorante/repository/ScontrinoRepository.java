package it.uniroma3.siw_ristorante.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import it.uniroma3.siw_ristorante.model.Scontrino;

public interface ScontrinoRepository extends JpaRepository<Scontrino, Long>{

    /* Ordinati per data del pagamento, dal piu' recente: e' l'ordine in cui si
       guarda un registro di cassa. Non per apertura, che e' ammessa nulla e
       manderebbe in fondo proprio gli scontrini piu' vecchi. */
    List<Scontrino> findByRistoranteIdOrderByDataOraDesc(Long ristoranteId);

    /* Filtrato dal ristorante, mai findById nudo: cambiando il numero
       nell'indirizzo non si legge la cassa di un altro locale. */
    Optional<Scontrino> findByIdAndRistoranteId(Long id, Long ristoranteId);

    /* ================= statistiche =================
       Tutte aggregazioni: contano e sommano nel database e riportano poche
       righe, invece di portare in memoria centinaia di scontrini per fare la
       somma qui. La differenza si vede quando il locale lavora da qualche anno.

       I risultati arrivano come PROIEZIONI (le interfacce in fondo) e non come
       Object[]: l'alias della select diventa il nome del getter, e chi legge il
       service trova getIncasso() invece di riga[2] con un cast. */

    /* Gli anni in cui c'e' almeno uno scontrino, dal piu' recente: e' quello
       che riempie il menu a tendina della pagina. Offrire anni vuoti vorrebbe
       dire offrire grafici vuoti. */
    @Query("""
            select distinct year(s.dataOra)
            from Scontrino s
            where s.ristorante.id = ?1
            order by year(s.dataOra) desc
            """)
    List<Integer> anniConScontrini(Long ristoranteId);

    @Query("select count(s) from Scontrino s where s.ristorante.id = ?1 and year(s.dataOra) = ?2")
    long contaPerAnno(Long ristoranteId, int anno);

    /* BigDecimal e non un primitivo perche' su un anno senza scontrini la
       somma non fa zero: non esiste, e il database risponde null. A decidere
       che cosa mostrare al posto del nulla e' il service. */
    @Query("select sum(s.totale) from Scontrino s where s.ristorante.id = ?1 and year(s.dataOra) = ?2")
    BigDecimal incassoPerAnno(Long ristoranteId, int anno);

    /* Quante porzioni sono uscite dalla cucina: si somma la quantita' delle
       righe, non si contano le righe (una riga sola puo' valere tre porzioni). */
    @Query("""
            select sum(r.quantita)
            from RigaScontrino r
            where r.scontrino.ristorante.id = ?1 and year(r.scontrino.dataOra) = ?2
            """)
    Long pietanzeVendutePerAnno(Long ristoranteId, int anno);

    /* Quanto dura in media un turno a tavola.

       "and s.apertura is not null" non e' ridondante: apertura e' ammessa
       nulla (gli scontrini piu' vecchi della funzionalita' non ce l'hanno) e
       senza il filtro quelle righe entrerebbero nella media come zeri,
       abbassandola. Escludere una riga e' diverso da contarla come zero.
       Double e non double: senza nessuna riga utile, la media e' null. */
    @Query("""
            select avg(timestampdiff(minute, s.apertura, s.dataOra))
            from Scontrino s
            where s.ristorante.id = ?1 and year(s.dataOra) = ?2 and s.apertura is not null
            """)
    Double durataMediaMinuti(Long ristoranteId, int anno);

    /* Conti chiusi e incasso, mese per mese. Tornano SOLO i mesi che hanno
       almeno uno scontrino: a riempire gli altri con degli zeri pensa il
       service, perche' un grafico a cui manca agosto non mostra agosto vuoto,
       salda luglio a settembre e racconta una cosa falsa. */
    @Query("""
            select month(s.dataOra) as mese, count(s) as numero, sum(s.totale) as incasso
            from Scontrino s
            where s.ristorante.id = ?1 and year(s.dataOra) = ?2
            group by month(s.dataOra)
            order by month(s.dataOra)
            """)
    List<AggregatoMese> perMese(Long ristoranteId, int anno);

    /* A che ora i clienti si siedono.

       coalesce(apertura, dataOra): quando l'orario di apertura manca si
       ripiega su quello del pagamento, cosi' gli scontrini vecchi
       contribuiscono con un'ora approssimata invece di sparire dal grafico.
       Qui, a differenza della durata media, un dato impreciso e' meglio di un
       dato assente: sposta una barra di un'ora, non falsa una media. */
    @Query("""
            select hour(coalesce(s.apertura, s.dataOra)) as ora, count(s) as numero
            from Scontrino s
            where s.ristorante.id = ?1 and year(s.dataOra) = ?2
            group by hour(coalesce(s.apertura, s.dataOra))
            order by hour(coalesce(s.apertura, s.dataOra))
            """)
    List<AggregatoOra> perOra(Long ristoranteId, int anno);

    /* La classifica dei piatti.

       Si legge da RigaScontrino e non da RigaOrdinazione: le ordinazioni
       spariscono alla chiusura del conto, le righe dello scontrino restano.

       group by r.piattoId con max(r.nomePiatto) come etichetta: il nome e' una
       copia salvata al momento della vendita, e se il piatto e' stato
       rinominato nel tempo, raggruppare anche per nome lo spezzerebbe in due
       voci che sono lo stesso piatto.

       Il limite arriva come Pageable: LIMIT non esiste in JPQL, e questo e' il
       modo con cui Spring Data lo esprime senza scendere a SQL nativo. */
    @Query("""
            select r.piattoId as piattoId, max(r.nomePiatto) as nome,
                   sum(r.quantita) as quantita,
                   sum(r.prezzoUnitario * r.quantita) as incasso
            from RigaScontrino r
            where r.scontrino.ristorante.id = ?1 and year(r.scontrino.dataOra) = ?2
            group by r.piattoId
            order by sum(r.quantita) desc, sum(r.prezzoUnitario * r.quantita) desc
            """)
    List<AggregatoPiatto> piattiPiuVenduti(Long ristoranteId, int anno, Pageable limite);

    /* Data e totale di ogni scontrino dell'anno, senza aggregare.

       E' l'unica statistica che NON si calcola nel database, ed e' una scelta:
       il giorno della settimana e' l'unico pezzo di calendario su cui i
       database non vanno d'accordo. In PostgreSQL extract(dow) conta
       0 = domenica, altrove 1 = domenica, e la convenzione che serve qui e'
       quella ISO (1 = lunedi). Raggruppare in Java con DayOfWeek.getValue()
       toglie l'ambiguita', e il prezzo e' portarsi in memoria qualche
       centinaio di righe di due colonne - un locale, un anno alla volta.
       L'errore che si evita sarebbe un grafico ruotato di un giorno: il genere
       di sbaglio che nessuno nota. */
    @Query("""
            select s.dataOra as dataOra, s.totale as totale
            from Scontrino s
            where s.ristorante.id = ?1 and year(s.dataOra) = ?2
            """)
    List<RigaCassa> cassaDellAnno(Long ristoranteId, int anno);

    /* ---------- proiezioni ----------
       Interfacce e non classi: Spring Data ne costruisce l'implementazione da
       solo, abbinando ogni alias della select al getter con lo stesso nome. */

    interface AggregatoMese {
        Integer getMese();

        Long getNumero();

        BigDecimal getIncasso();
    }

    interface AggregatoOra {
        Integer getOra();

        Long getNumero();
    }

    interface AggregatoPiatto {
        Long getPiattoId();

        String getNome();

        Long getQuantita();

        BigDecimal getIncasso();
    }

    interface RigaCassa {
        LocalDateTime getDataOra();

        BigDecimal getTotale();
    }
}
