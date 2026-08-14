package com.binar.bc.saku_ku.exception;

import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpStatus;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;


@Component
@RequiredArgsConstructor

public class UnauthorizedHandler implements AuthenticationEntryPoint{

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
        HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException authException) throws IOException {
            response(response, GlobalExceptionHandler.REQUIRED_AUTHENTICATION_MESSAGE);  
        
        }

    public void response(
        HttpServletResponse response, String message) throws IOException {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            objectMapper.writeValue(
                response.getWriter(),
                GlobalExceptionHandler.body(HttpStatus.UNAUTHORIZED, message)
            );
        }
}
