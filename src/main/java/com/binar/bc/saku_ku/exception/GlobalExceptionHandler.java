package com.binar.bc.saku_ku.exception;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;
import io.jsonwebtoken.SignatureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import jakarta.persistence.EntityNotFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    public static final String REQUIRED_AUTHENTICATION_MESSAGE = "Unauthorized access: Authentication required";

    // Sebelum ini, kegagalan @Valid (field kosong/salah format dkk) jatuh ke default bawaan
    // ResponseEntityExceptionHandler (bentuk ProblemDetail RFC 7807) - beda total sama bentuk
    // {timestamp,status,error,message} yang dipakai semua exception lain di file ini. Override
    // ini nyamain bentuknya, jadi SEMUA 400 di API ini punya 1 bentuk konsisten - juga berarti
    // dokumentasi Swagger buat "400" akhirnya beneran akurat buat endpoint POST/PATCH manapun.
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(body(HttpStatus.BAD_REQUEST, message.isBlank() ? "Input tidak valid" : message));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> illegalArgument 
        (IllegalArgumentException e) {
            return build(HttpStatus.BAD_REQUEST, e.getMessage());
        }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, Object>> handleUnautrhorizedException(UnauthorizedException e) {
        return build(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleEntityNotFoundException(EntityNotFoundException e) {
        return build(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessRuleException(BusinessRuleException e) {
        return build(HttpStatus.UNPROCESSABLE_CONTENT, e.getMessage());
    }

    // DataIntegrityViolationException.getMessage() includes the raw SQL + bind values + DB
    // error detail (Hibernate/Postgres nests the whole failed statement in there) - letting the
    // generic RuntimeException handler below pass that straight to e.getMessage() would leak
    // table/column names and row data to the client. Caught separately here on purpose, with a
    // message that doesn't repeat what went wrong at the SQL level.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        return build(HttpStatus.UNPROCESSABLE_CONTENT, "Data tidak valid atau bentrok dengan data yang sudah ada");
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException e) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }

        @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<Map<String , Object>> handleUsernameNotFound(UsernameNotFoundException e) {
        String message = (e.getMessage() != null && !e.getMessage().isBlank())
                ? e.getMessage()
                : "User tidak ditemukan";
        return build(HttpStatus.UNAUTHORIZED, message);
    }
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handlerBadCredentialsException(BadCredentialsException e){
        return build(HttpStatus.UNAUTHORIZED, "Username atau Password salah");
    }
    @ExceptionHandler(SignatureException.class)
    public ResponseEntity<Map<String, Object>> handlerSignatureException(SignatureException e){
        return build(HttpStatus.UNAUTHORIZED, "JWT tidak ditemukan (?)");
    }

    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String message){
        return ResponseEntity.status(status).body(body(status,message));
    }

    public static Map<String, Object> body(HttpStatus status, String message){
        return Map.of(
                "timestamp", Instant.now().toString(),
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", message == null ? "" : message);
    }
}