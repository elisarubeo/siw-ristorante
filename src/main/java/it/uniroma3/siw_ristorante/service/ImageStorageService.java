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

@Service
public class ImageStorageService {

    private static final Logger logger = LoggerFactory.getLogger(ImageStorageService.class);

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

    private Path risolvi(String nomeFile) {
        Path percorso = this.directory.resolve(nomeFile).normalize();
        if (!percorso.startsWith(this.directory)) {
            throw new InvalidImageException("Nome di file non valido: " + nomeFile);
        }
        return percorso;
    }
}
