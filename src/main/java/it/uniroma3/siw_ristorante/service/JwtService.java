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

@Service
public class JwtService {

    /* Il nome del claim con il ruolo. Una costante e non la stringa ripetuta
       in tre punti: scrivendola male in uno solo, il token risulterebbe valido
       e l'utente senza permessi. */
    private static final String CLAIM_RUOLO = "role";

    private final SecretKey chiave;
    private final long durataMs;

    public JwtService(@Value("${app.jwt.secret}") String segreto,
            @Value("${app.jwt.expiration-ms}") long durataMs) {
        this.chiave = Keys.hmacShaKeyFor(segreto.getBytes(StandardCharsets.UTF_8));
        this.durataMs = durataMs;
    }

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

    public String estraiUsername(String token) {
        Claims claims = leggi(token);
        return claims == null ? null : claims.getSubject();
    }

    public String estraiRuolo(String token) {
        Claims claims = leggi(token);
        return claims == null ? null : claims.get(CLAIM_RUOLO, String.class);
    }

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
