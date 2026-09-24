package com.binar.bc.saku_ku.exception;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import tools.jackson.databind.ObjectMapper;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UnauthorizedHandlerTest {

    @Mock
    private HttpServletResponse response;

    private UnauthorizedHandler handler;

    @BeforeEach
    void setUp() throws Exception {
        handler = new UnauthorizedHandler(new ObjectMapper());
        StringWriter buffer = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(buffer));
    }

    @Test
    void response_writesUnauthorizedJsonBody() throws Exception {
        handler.response(response, "Token tidak valid");

        verify(response).setStatus(HttpStatus.UNAUTHORIZED.value());
        verify(response).setContentType("application/json");
    }

    @Test
    void commence_delegatesToResponse_withDefaultMessage() throws Exception {
        handler.commence(null, response, new BadCredentialsException("bad creds"));

        verify(response).setStatus(HttpStatus.UNAUTHORIZED.value());
    }
}
