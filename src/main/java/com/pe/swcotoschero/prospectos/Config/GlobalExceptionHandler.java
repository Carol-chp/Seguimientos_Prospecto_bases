package com.pe.swcotoschero.prospectos.Config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.validation.ConstraintViolationException;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Manejo global de errores (versión mínima — Fase 1).
 *
 * Sin esto, una excepción no capturada se propaga y el ExceptionTranslationFilter
 * de Spring Security la convierte en un 403 opaco con body vacío, lo que hace
 * imposible para el frontend (wizard) distinguir un error de validación de uno
 * de permisos. Aquí mapeamos los casos comunes a respuestas JSON claras.
 *
 * La versión completa (códigos por tipo, trazas estructuradas, i18n) es Fase 3.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static Map<String, Object> body(HttpStatus status, String mensaje) {
        return Map.of(
                "timestamp", OffsetDateTime.now().toString(),
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", mensaje == null ? status.getReasonPhrase() : mensaje);
    }

    /** Reglas de negocio / validación de datos → 400 Bad Request (con el mensaje). */
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<Map<String, Object>> handleBadRequest(RuntimeException ex) {
        log.warn("Solicitud inválida: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(body(HttpStatus.BAD_REQUEST, ex.getMessage()));
    }

    /** Bean Validation en @RequestBody (@Valid) → 400 con errores por campo. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleBodyValidation(
            MethodArgumentNotValidException ex) {
        Map<String, String> errores = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() == null ? "inválido" : fe.getDefaultMessage(),
                        (a, b) -> a, LinkedHashMap::new));
        log.warn("Validación de cuerpo fallida: {}", errores);
        Map<String, Object> b = body(HttpStatus.BAD_REQUEST, "Datos inválidos.");
        Map<String, Object> r = new LinkedHashMap<>(b);
        r.put("errores", errores);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(r);
    }

    /** Bean Validation en parámetros (@RequestParam/@PathVariable) → 400. */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraint(
            ConstraintViolationException ex) {
        Map<String, String> errores = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        v -> {
                            String p = v.getPropertyPath().toString();
                            int dot = p.lastIndexOf('.');
                            return dot >= 0 ? p.substring(dot + 1) : p;
                        },
                        v -> v.getMessage() == null ? "inválido" : v.getMessage(),
                        (a, b) -> a, LinkedHashMap::new));
        log.warn("Validación de parámetros fallida: {}", errores);
        Map<String, Object> b = body(HttpStatus.BAD_REQUEST, "Parámetros inválidos.");
        Map<String, Object> r = new LinkedHashMap<>(b);
        r.put("errores", errores);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(r);
    }

    /**
     * Errores de seguridad: se preservan sus códigos (NO convertir a 500/400).
     * Spring Security normalmente los maneja a nivel de filtro, pero si llegan
     * por @PreAuthorize dentro del dispatch hay que respetar 401/403.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(body(HttpStatus.FORBIDDEN, "No tiene permisos para esta operación."));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuth(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(body(HttpStatus.UNAUTHORIZED, "No autenticado."));
    }

    /** Cualquier otra excepción → 500 sin filtrar el stack trace al cliente. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        log.error("Error no controlado", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(body(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Error interno. Contacte al administrador."));
    }
}
