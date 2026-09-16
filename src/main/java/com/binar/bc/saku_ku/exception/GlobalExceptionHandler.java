package com.binar.bc.saku_ku.exception;

import java.time.Instant;
import java.util.Map;
import io.jsonwebtoken.SignatureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import jakarta.persistence.EntityNotFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    
    public static final String REQUIRED_AUTHENTICATION_MESSAGE = "Unauthorized access: Authentication required";

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

    // Catch-all buat runtime exception yang gak ke-handle handler spesifik manapun di atas
    // (mis. MailAuthenticationException pas OTP register gagal kirim). Sebelumnya di-map ke
    // 404 - salah kaprah, bikin error server-side (SMTP gagal auth, dst) nyamar jadi "resource
    // gak ketemu" di sisi client. 500 lebih bener secara semantik: ini genuinely unexpected
    // server error, bukan client salah alamat/ID.
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