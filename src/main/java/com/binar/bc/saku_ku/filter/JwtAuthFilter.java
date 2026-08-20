package com.binar.bc.saku_ku.filter;

import com.binar.bc.saku_ku.entity.AppCustomerEntity;
import com.binar.bc.saku_ku.entity.AppUserEntity;
import com.binar.bc.saku_ku.exception.UnauthorizedException;
import com.binar.bc.saku_ku.exception.UnauthorizedHandler;
import com.binar.bc.saku_ku.service.AppCustomerDetailsService;
import com.binar.bc.saku_ku.service.AppUserDetailsService;
import com.binar.bc.saku_ku.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String PREFIX = "Bearer ";
    private static final String INVALID_TOKEN = "Token tidak valid";

    private final JwtService jwtService;
    private final AppUserDetailsService appUserDetailsService;
    private final AppCustomerDetailsService appCustomerDetailsService;
    private final UnauthorizedHandler unauthorizedHandler;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        System.out.println("=== DEBUG HEADER: [" + header + "] ===");
        if (header == null || !header.startsWith(PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Claims claims = jwtService.parse(header.substring(PREFIX.length()));
            String role = claims.get("role", String.class);
            String subject = claims.getSubject();

            UsernamePasswordAuthenticationToken authentication;

            if ("CUSTOMER".equals(role)) {
                AppCustomerEntity customer = appCustomerDetailsService.findCustomer(subject)
                        .orElseThrow(() -> new UnauthorizedException(INVALID_TOKEN));
                authentication = new UsernamePasswordAuthenticationToken(
                        customer, null, customer.getAuthorities());
            } else {
                AppUserEntity user = appUserDetailsService.findUser(subject)
                        .orElseThrow(() -> new UnauthorizedException(INVALID_TOKEN));
                authentication = new UsernamePasswordAuthenticationToken(
                        user, null, user.getAuthorities());
            }

            SecurityContextHolder.getContext().setAuthentication(authentication);
System.out.println("=== AUTH SET: " + authentication.getName() + " | AUTHORITIES: " + authentication.getAuthorities() + " ===");
        } catch (JwtException | UsernameNotFoundException | IllegalArgumentException e) {
            SecurityContextHolder.clearContext();
            unauthorizedHandler.response(response, INVALID_TOKEN);
            return;
        }

        filterChain.doFilter(request, response);
    }
}


// package com.binar.bc.saku_ku.filter;
// import org.springframework.http.HttpHeaders;
// import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
// import org.springframework.security.core.context.SecurityContextHolder;
// import org.springframework.stereotype.Component;
// import jakarta.servlet.http.HttpServletResponse;
// import org.springframework.security.core.userdetails.UsernameNotFoundException;
// import com.binar.bc.saku_ku.entity.AppUserEntity;
// import com.binar.bc.saku_ku.exception.UnauthorizedHandler;
// import com.binar.bc.saku_ku.exception.UnauthorizedException;
// import com.binar.bc.saku_ku.service.AppUserDetailsService;
// import com.binar.bc.saku_ku.service.JwtService;
// import io.jsonwebtoken.Claims;
// import io.jsonwebtoken.JwtException;
// import jakarta.servlet.FilterChain;
// import jakarta.servlet.ServletException;
// import java.io.IOException;
// import org.springframework.web.filter.OncePerRequestFilter;
// import jakarta.servlet.http.HttpServletRequest;
// import lombok.RequiredArgsConstructor;

// @Component
// @RequiredArgsConstructor
// public class JwtAuthFilter extends OncePerRequestFilter {

//     private static final String PREFIX = "Bearer ";
//     private static final String INVALID_TOKEN = "Token tidak valid";

//     private final JwtService jwtService;
//     private final AppUserDetailsService appUserDetailsService;
//     private final UnauthorizedHandler unauthorizedHandler;

//     @Override

//     protected void doFilterInternal (
//         HttpServletRequest request,
//         HttpServletResponse response,
//         FilterChain filterchain)
//         throws ServletException, IOException {

//             String header = request.getHeader(HttpHeaders.AUTHORIZATION);
//             if (header == null || !header.startsWith(PREFIX)) {
//                 filterchain.doFilter(request, response);
//                 return;
//             }

//             try {
//                 Claims claims = jwtService.parse(header.substring(PREFIX.length()));
//                 AppUserEntity user = appUserDetailsService.findUser(claims.getSubject())
//                     .orElseThrow(() -> new UnauthorizedException(INVALID_TOKEN));
//                     UsernamePasswordAuthenticationToken authentication = 
//                     new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

//                     SecurityContextHolder.getContext().setAuthentication(authentication);
//             } catch (JwtException | UsernameNotFoundException | IllegalArgumentException e) {
//                    SecurityContextHolder.clearContext();
//                    unauthorizedHandler.response(response, INVALID_TOKEN);
//                    return; 
//             }
            
//             filterchain.doFilter(request, response);
//         }

// }

