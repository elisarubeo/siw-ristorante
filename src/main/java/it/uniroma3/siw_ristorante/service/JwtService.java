package it.uniroma3.siw_ristorante.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/* Crea e verifica i token della parte REST.

   CHE COS'E' UN JWT. Tre pezzi separati da un punto, ognuno in Base64URL:
   intestazione, contenuto e firma. I primi due NON sono cifrati - chiunque
   abbia il token li puo' leggere incollandolo su un sito qualsiasi. Quello che
   la firma garantisce e' l'integrita': cambiando anche un carattere del
   contenuto la firma non torna piu', e il token viene rifiutato. Per questo
   dentro ci vanno solo lo username e il ruolo, mai la password e mai niente
   che debba restare segreto.

   PERCHE' UN TOKEN E NON LA SESSIONE. La meta' Thymeleaf del sito riconosce
   l'utente da un cookie JSESSIONID, e il server tiene in memoria a chi
   corrisponde. Qui invece il server non ricorda niente fra una richiesta e
   l'altra: e' il token, che il browser allega ogni volta, a contenere tutto il
   necessario per ricostruire chi sta chiamando. E' questo il senso di
   "stateless", e il motivo per cui la catena /api e' configurata con
   SessionCreationPolicy.STATELESS. */
@Service
public class JwtService {

    /* Il nome del claim con il ruolo. Una costante e non la stringa ripetuta
       in tre punti: scrivendola male in uno solo, il token risulterebbe valido
       e l'utente senza permessi. */
    private static final String CLAIM_RUOLO = "role";

    private final SecretKey chiave;
    private final long durataMs;

    /* Il segreto e la durata arrivano da application.properties: una chiave
       scritta nel codice finirebbe su Git, e cambiarla vorrebbe dire
       ricompilare.

       Keys.hmacShaKeyFor pretende almeno 32 byte (256 bit, la misura di
       HMAC-SHA256) e rifiuta le chiavi piu' corte lanciando SUBITO, all'avvio
       dell'applicazione. E' un bene: meglio un'applicazione che non parte di
       una che parte e firma con una chiave debole. */
    public JwtService(@Value("${app.jwt.secret}") String segreto,
            @Value("${app.jwt.expiration-ms}") long durataMs) {
        this.chiave = Keys.hmacShaKeyFor(segreto.getBytes(StandardCharsets.UTF_8));
        this.durataMs = durataMs;
    }

    /* Il token consegnato al login. subject e' il posto standard dove mettere
       "di chi stiamo parlando"; il ruolo e' un claim nostro.

       La scadenza non e' un dettaglio: un token rubato vale finche' non scade,
       e non c'e' modo di revocarlo - il server non tiene un elenco dei token
       emessi, e' proprio quello a cui si rinuncia scegliendo stateless. */
    public String generaToken(String username, String ruolo) {
        Instant adesso = Instant.now();
        return Jwts.builder()
                .subject(username)
                .claim(CLAIM_RUOLO, ruolo)
                .issuedAt(Date.from(adesso))
                .expiration(Date.from(adesso.plusMillis(durataMs)))
                .signWith(chiave)
                .compact();
    }

    /* Chi ha firmato questo token, o null se non e' valido.

       null e non un'eccezione: chi chiama e' il filtro, che su un token
       assente o scaduto non deve bloccare niente, deve solo lasciar passare la
       richiesta come anonima. Un'eccezione lo costringerebbe a un try/catch
       per esprimere il caso normale. */
    public String estraiUsername(String token) {
        Claims claims = leggi(token);
        return claims == null ? null : claims.getSubject();
    }

    public String estraiRuolo(String token) {
        Claims claims = leggi(token);
        return claims == null ? null : claims.get(CLAIM_RUOLO, String.class);
    }

    /* Verifica la firma e la scadenza, e restituisce il contenuto.

       verifyWith(chiave) e' la riga che conta: senza, si leggerebbe il
       contenuto di QUALUNQUE token, comprese le cose che chiunque puo'
       fabbricarsi scrivendo tre pezzi in Base64. Leggere e verificare devono
       essere lo stesso gesto, altrimenti prima o poi si legge senza
       verificare. */
    private Claims leggi(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            return Jwts.parser()
                    .verifyWith(chiave)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            /* Firma sbagliata, token scaduto, formato incomprensibile: per chi
               chiama sono la stessa cosa, cioe' "non mi fido di questo
               token". Distinguerli servirebbe solo a dire a un eventuale
               attaccante quanto c'e' andato vicino. */
            return null;
        }
    }
}
