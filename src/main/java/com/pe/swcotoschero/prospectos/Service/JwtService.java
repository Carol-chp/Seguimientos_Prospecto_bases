package com.pe.swcotoschero.prospectos.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    /** The well-known development default — rejected in prod. */
    private static final String DEV_DEFAULT_SECRET =
            "dev-only-secret-must-be-at-least-32-chars-long!";

    /**
     * Clave secreta leida desde variable de entorno JWT_SECRET.
     * En dev se usa el valor por defecto del application-dev.properties.
     * En prod DEBE venir por variable de entorno; el arranque falla si no se configura.
     */
    @Value("${app.jwt.secret}")
    private String jwtSecret;

    private final Environment environment;

    /** Cached SecretKey — built once in @PostConstruct, never rebuilt per call. */
    private SecretKey signingKey;

    public JwtService(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    void initSigningKey() {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException(
                    "app.jwt.secret no puede estar vacío. Configure JWT_SECRET en el entorno.");
        }
        if (jwtSecret.length() < 32) {
            throw new IllegalStateException(
                    "app.jwt.secret debe tener al menos 32 caracteres. "
                    + "Longitud actual: " + jwtSecret.length());
        }
        boolean isProd = Arrays.asList(environment.getActiveProfiles()).contains("prod");
        if (DEV_DEFAULT_SECRET.equals(jwtSecret)) {
            if (isProd) {
                throw new IllegalStateException(
                        "app.jwt.secret usa el valor por defecto de desarrollo. "
                        + "Este secreto es publico y NO debe usarse en produccion. "
                        + "Configure JWT_SECRET con un valor seguro.");
            } else {
                log.warn("ADVERTENCIA DE SEGURIDAD: app.jwt.secret usa el valor por defecto de "
                        + "desarrollo. Cambielo antes de desplegar en produccion.");
            }
        }
        this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    private SecretKey getSigningKey() {
        return signingKey;
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 8)) // 8 horas
                .signWith(getSigningKey())
                .compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }
}
