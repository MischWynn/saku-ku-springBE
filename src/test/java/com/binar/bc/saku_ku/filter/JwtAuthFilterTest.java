package com.binar.bc.saku_ku.filter;

import com.binar.bc.saku_ku.entity.AppCustomerEntity;
import com.binar.bc.saku_ku.entity.AppUserEntity;
import com.binar.bc.saku_ku.exception.UnauthorizedHandler;
import com.binar.bc.saku_ku.service.AppCustomerDetailsService;
import com.binar.bc.saku_ku.service.AppUserDetailsService;
import com.binar.bc.saku_ku.service.JwtService;
import com.binar.bc.saku_ku.service.TokenBlacklistService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock
    private JwtService jwtService;
    @Mock
    private AppUserDetailsService appUserDetailsService;
    @Mock
    private AppCustomerDetailsService appCustomerDetailsService;
    @Mock
    private UnauthorizedHandler unauthorizedHandler;
    @Mock
    private TokenBlacklistService tokenBlacklistService;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;
    @Mock
    private Claims claims;

    private JwtAuthFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthFilter(jwtService, appUserDetailsService, appCustomerDetailsService,
                unauthorizedHandler, tokenBlacklistService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_passesThrough_whenNoAuthorizationHeader() throws Exception {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(jwtService, never()).parse(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void doFilterInternal_passesThrough_whenHeaderNotBearer() throws Exception {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Basic abc123");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_setsCustomerAuthentication_whenRoleIsCustomer() throws Exception {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer valid-token");
        when(tokenBlacklistService.isBlacklisted("valid-token")).thenReturn(false);
        when(jwtService.parse("valid-token")).thenReturn(claims);
        when(claims.get("role", String.class)).thenReturn("CUSTOMER");
        when(claims.getSubject()).thenReturn("nasabah@mail.com");
        AppCustomerEntity customer = new AppCustomerEntity();
        customer.setId(UUID.randomUUID());
        customer.setUsername("nasabah@mail.com");
        when(appCustomerDetailsService.findCustomer("nasabah@mail.com")).thenReturn(Optional.of(customer));

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isSameAs(customer);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_setsStaffAuthentication_whenRoleNotCustomer() throws Exception {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer valid-token");
        when(tokenBlacklistService.isBlacklisted("valid-token")).thenReturn(false);
        when(jwtService.parse("valid-token")).thenReturn(claims);
        when(claims.get("role", String.class)).thenReturn("MARKETING");
        when(claims.getSubject()).thenReturn("dewi.marketing");
        AppUserEntity user = new AppUserEntity();
        user.setUsername("dewi.marketing");
        user.setRole("MARKETING");
        when(appUserDetailsService.findUser("dewi.marketing")).thenReturn(Optional.of(user));

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isSameAs(user);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_rejectsRequest_whenTokenBlacklisted() throws Exception {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer blacklisted-token");
        when(tokenBlacklistService.isBlacklisted("blacklisted-token")).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        verify(unauthorizedHandler).response(response, "Token tidak valid");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void doFilterInternal_rejectsRequest_whenJwtInvalid() throws Exception {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer garbage-token");
        when(tokenBlacklistService.isBlacklisted("garbage-token")).thenReturn(false);
        when(jwtService.parse("garbage-token")).thenThrow(new JwtException("malformed"));

        filter.doFilterInternal(request, response, filterChain);

        verify(unauthorizedHandler).response(response, "Token tidak valid");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void doFilterInternal_rejectsRequest_whenStaffNotFound() throws Exception {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer valid-token");
        when(tokenBlacklistService.isBlacklisted("valid-token")).thenReturn(false);
        when(jwtService.parse("valid-token")).thenReturn(claims);
        when(claims.get("role", String.class)).thenReturn("MARKETING");
        when(claims.getSubject()).thenReturn("ghost");
        when(appUserDetailsService.findUser("ghost")).thenReturn(Optional.empty());

        filter.doFilterInternal(request, response, filterChain);

        verify(unauthorizedHandler).response(response, "Token tidak valid");
        verify(filterChain, never()).doFilter(request, response);
    }
}
