package com.binar.bc.saku_ku.service;

import com.binar.bc.saku_ku.dto.AuthResponseDTO;
import com.binar.bc.saku_ku.dto.CustomerLoginRequest;
import com.binar.bc.saku_ku.dto.CustomerRegisterRequest;
import com.binar.bc.saku_ku.dto.CustomerResponseDTO;
import com.binar.bc.saku_ku.entity.AppCustomerEntity;
import com.binar.bc.saku_ku.entity.CustomerEntity;
import com.binar.bc.saku_ku.exception.BusinessRuleException;
import com.binar.bc.saku_ku.exception.UnauthorizedException;
import com.binar.bc.saku_ku.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomerAuthService {

    private final CustomerRepository customerRepository;
    private final AppCustomerDetailsService appCustomerDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserPlafondService userPlafondService;

    public CustomerResponseDTO register(CustomerRegisterRequest request) {
        if (customerRepository.existsByEmail(request.getEmail())) {
            throw new BusinessRuleException("Email sudah terdaftar");
        }
        if (customerRepository.existsByNoHp(request.getNoHp())) {
            throw new BusinessRuleException("Nomor HP sudah terdaftar");
        }
        if (customerRepository.existsByNik(request.getNik())) {
            throw new BusinessRuleException("NIK sudah terdaftar");
        }

        CustomerEntity customer = new CustomerEntity();
        customer.setNamaLengkap(request.getNamaLengkap());
        customer.setNik(request.getNik());
        customer.setNoHp(request.getNoHp());
        customer.setEmail(request.getEmail());
        customer.setAlamat(request.getAlamat());
        customer.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        customer.setPlafond(BigDecimal.ZERO);
        customer.setStatus("ACTIVE");
        customer.setTanggalLahir(request.getTanggalLahir());
        customer.setTipePekerjaan(request.getTipePekerjaan());
        customer.setPekerjaan(request.getPekerjaan());
        customer.setLamaBekerjaBulan(request.getLamaBekerjaBulan());
        customer.setPendapatanBulanan(request.getPendapatanBulanan());
        customer.setUtangBerjalan(request.getUtangBerjalan());

        CustomerEntity saved = customerRepository.save(customer);
        userPlafondService.calculateAndAssign(saved);
        saved = customerRepository.save(saved); // persist plafond sync dari calculateAndAssign
        return CustomerResponseDTO.from(saved);
    }

    public ResponseEntity<AuthResponseDTO> login(CustomerLoginRequest request) {
        Optional<AppCustomerEntity> found = appCustomerDetailsService.findCustomer(request.getIdentifier());

        if (found.isEmpty() || !passwordEncoder.matches(request.getPassword(), found.get().getPassword())) {
            throw new UnauthorizedException("Email/No HP atau password salah");
        }

        AppCustomerEntity customer = found.get();
        String token = jwtService.issue(customer.getUsername(), customer.getRole(), Instant.now());
        return ResponseEntity.ok(new AuthResponseDTO(token));
    }
}