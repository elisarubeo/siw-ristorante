package it.uniroma3.siw_ristorante.dto;

public record LoginResponse(
        String token,
        String username,
        Long userId,
        String role) {
}
