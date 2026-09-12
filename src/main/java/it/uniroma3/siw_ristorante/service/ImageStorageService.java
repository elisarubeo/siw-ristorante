package it.uniroma3.siw_ristorante.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import it.uniroma3.siw_ristorante.exception.InvalidImageException;

/* Salvataggio dei file immagine su disco.

   E' l'unico punto dell'applicazione che tocca il filesystem: gli altri
   service gli chiedono di scrivere o cancellare un file e maneggiano soltanto
   un nome. Nel database non finiscono i byte delle immagini ma solo quel nome,
   nella collezione Ristorante.immagini.

   LA COSA DA SAPERE DI QUESTA CLASSE: scrivere un file NON e' un'operazione
   transazionale. Se la transazione che ha chiesto il salvataggio viene
   annullata, il file resta sul disco lo stesso. Per questo chi lo usa
   (RistoranteService) lega la cancellazione all'esito della transazione,
   invece di cancellare subito. */
@Service
public class ImageStorageService {

    private static final Logger logger = LoggerFactory.getLogger(ImageStorageService.class);

    /* Formati ammessi, con l'estensione da usare per ciascuno.

       L'estensione la decide questa mappa e non il nome del file caricato:
       quel nome arriva dal browser e non merita fiducia - puo' contenere un
       percorso ("../../application.properties") oppure un'estensione che non
       corrisponde al contenuto. */
    private static final Map<String, String> ESTENSIONI = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp",
            "image/gif", "gif");

    private final Path directory;

    public ImageStorageService(@Value("${app.uploads.directory}") String directory) {
        /* Percorso assoluto e normalizzato una volta sola, all'avvio: serve
           come riferimento per il controllo anti path traversal piu' sotto. */
        this.directory = Paths.get(directory).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.directory);
        } catch (IOException e) {
            /* Senza cartella non si puo' salvare niente: meglio che
               l'applicazione non parta, invece di scoprirlo al primo
               caricamento. */
            throw new UncheckedIOException(
                    "Impossibile creare la cartella delle immagini: " + this.directory, e);
        }
        logger.info("Le immagini caricate vengono salvate in {}", this.directory);
    }

    /* Il file e' un'immagine di un formato che sappiamo gestire?

       Serve al controller per segnalare l'errore PRIMA di salvare qualsiasi
       cosa: la regola vive qui, dove stanno i formati, e il controller si
       limita a chiederlo. */
    public boolean isSupported(MultipartFile file) {
        return file != null && !file.isEmpty() && ESTENSIONI.containsKey(file.getContentType());
    }

    /* Salva il file e restituisce il nome con cui e' stato scritto: e' quello
       che va memorizzato nel database.

       Il nome e' un UUID generato qui, non quello scelto da chi carica: due
       ristoratori che caricano "sala.jpg" non si sovrascrivono a vicenda, e
       nessun nome proveniente dall'esterno finisce mai in un percorso. */
    public String store(MultipartFile file) {
        /* Ricontrollato anche qui e non solo nel controller: questo metodo e'
           pubblico e non puo' fidarsi di chi lo chiama. */
        if (!isSupported(file)) {
            throw new InvalidImageException(
                    "Il file non e' un'immagine valida: sono ammessi JPG, PNG, WEBP e GIF.");
        }

        String nomeFile = UUID.randomUUID() + "." + ESTENSIONI.get(file.getContentType());

        try (InputStream contenuto = file.getInputStream()) {
            Files.copy(contenuto, risolvi(nomeFile), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Salvataggio dell'immagine non riuscito", e);
        }

        return nomeFile;
    }

    /* Cancella un file, se esiste. Un nome null o vuoto non e' un errore: non
       c'e' semplicemente nulla da fare. */
    public void delete(String nomeFile) {
        if (nomeFile == null || nomeFile.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(risolvi(nomeFile));
        } catch (IOException e) {
            /* Un file rimasto sul disco e' spiacevole ma non compromette lo
               stato dell'applicazione: si annota nei log e si prosegue, invece
               di far fallire un'operazione che sui dati e' gia' riuscita. */
            logger.warn("Impossibile eliminare il file {}: {}", nomeFile, e.getMessage());
        }
    }

    /* Usata da UploadConfig per sapere da dove servire /uploads/**. */
    public Path getDirectory() {
        return this.directory;
    }

    /* Costruisce il percorso di un file dentro la cartella delle immagini,
       verificando che ci resti davvero dentro.

       E' la difesa dal path traversal: un nome come "../../segreto" sarebbe
       risolto fuori dalla cartella, e qui viene respinto. I nomi li generiamo
       noi, ma il controllo costa nulla e vale anche per quelli che arrivano
       dalla form di eliminazione, dove il nome del file viaggia nella
       richiesta e quindi puo' essere manomesso. */
    private Path risolvi(String nomeFile) {
        Path percorso = this.directory.resolve(nomeFile).normalize();
        if (!percorso.startsWith(this.directory)) {
            throw new InvalidImageException("Nome di file non valido: " + nomeFile);
        }
        return percorso;
    }
}
