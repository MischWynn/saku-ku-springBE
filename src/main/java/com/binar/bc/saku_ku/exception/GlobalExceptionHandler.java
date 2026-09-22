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
import lombok.extern.slf4j.Slf4j;

import jakarta.persistence.EntityNotFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    public static final String REQUIRED_AUTHENTICATION_MESSAGE = "Unauthorized access: Authentication required";

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

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        return build(HttpStatus.UNPROCESSABLE_CONTENT, "Data tidak valid atau bentrok dengan data yang sudah ada");
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException e) {
        log.error("Unhandled RuntimeException: {}", e.getMessage(), e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());

    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException e) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, "Layanan pihak ketiga sedang tidak tersedia, coba lagi nanti");
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