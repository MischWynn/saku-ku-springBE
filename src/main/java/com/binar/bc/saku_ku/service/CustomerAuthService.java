package com.binar.bc.saku_ku.service;

import com.binar.bc.saku_ku.dto.AuthResponseDTO;
import com.binar.bc.saku_ku.dto.CustomerForgotPasswordRequest;
import com.binar.bc.saku_ku.dto.CustomerLoginRequest;
import com.binar.bc.saku_ku.dto.CustomerRegisterRequest;
import com.binar.bc.saku_ku.dto.CustomerResetPasswordRequest;
import com.binar.bc.saku_ku.dto.CustomerResponseDTO;
import com.binar.bc.saku_ku.dto.CustomerUpdateRequest;
import com.binar.bc.saku_ku.dto.ResendOtpRequest;
import com.binar.bc.saku_ku.dto.VerifyOtpRequest;
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
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomerAuthService {

    private static final String PURPOSE_REGISTER_VERIFY = "REGISTER_VERIFY";
    private static final String PURPOSE_PASSWORD_RESET = "PASSWORD_RESET";

    private final CustomerRepository customerRepository;
    private final AppCustomerDetailsService appCustomerDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserPlafondService userPlafondService;
    private final OtpService otpService;
    private final PengajuanService pengajuanService;

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
        // Akun baru MENUNGGU verifikasi OTP dulu sebelum bisa login — lihat check di login().
        customer.setStatus("PENDING_VERIFICATION");
        customer.setTanggalLahir(request.getTanggalLahir());
        customer.setTipePekerjaan(request.getTipePekerjaan());
        customer.setPekerjaan(request.getPekerjaan());
        customer.setLamaBekerjaBulan(request.getLamaBekerjaBulan());
        customer.setPendapatanBulanan(request.getPendapatanBulanan());
        customer.setUtangBerjalan(request.getUtangBerjalan());

        CustomerEntity saved = customerRepository.save(customer);
        userPlafondService.calculateAndAssign(saved);
        saved = customerRepository.save(saved); // persist plafond sync dari calculateAndAssign

        otpService.generateAndSend(saved.getEmail(), PURPOSE_REGISTER_VERIFY);

        return CustomerResponseDTO.from(saved);
    }

    public ResponseEntity<AuthResponseDTO> login(CustomerLoginRequest request) {
        Optional<AppCustomerEntity> found = appCustomerDetailsService.findCustomer(request.getIdentifier());

        if (found.isEmpty() || !passwordEncoder.matches(request.getPassword(), found.get().getPassword())) {
            throw new UnauthorizedException("Email/No HP atau password salah");
        }

        AppCustomerEntity customer = found.get();

        // AppCustomerEntity (principal) gak bawa status — cek langsung ke entity aslinya.
        CustomerEntity fullCustomer = customerRepository.findById(customer.getId())
                .orElseThrow(() -> new UnauthorizedException("Email/No HP atau password salah"));
        if (!"ACTIVE".equals(fullCustomer.getStatus())) {
            throw new UnauthorizedException("Akun belum diverifikasi. Cek email Anda untuk kode OTP, atau minta kode baru.");
        }

        String token = jwtService.issue(customer.getUsername(), customer.getRole(), Instant.now());
        return ResponseEntity.ok(new AuthResponseDTO(token));
    }

    public CustomerResponseDTO verifyRegistrationOtp(VerifyOtpRequest request) {
        otpService.verify(request.getEmail(), PURPOSE_REGISTER_VERIFY, request.getCode());

        CustomerEntity customer = customerRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessRuleException("Customer tidak ditemukan"));
        customer.setStatus("ACTIVE");
        CustomerEntity saved = customerRepository.save(customer);
        return CustomerResponseDTO.from(saved);
    }

    public void resendRegistrationOtp(ResendOtpRequest request) {
        CustomerEntity customer = customerRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessRuleException("Email tidak ditemukan"));
        if ("ACTIVE".equals(customer.getStatus())) {
            throw new BusinessRuleException("Akun sudah terverifikasi");
        }
        otpService.generateAndSend(customer.getEmail(), PURPOSE_REGISTER_VERIFY);
    }

    public void forgotPassword(CustomerForgotPasswordRequest request) {
        // Tetap cek dulu emailnya beneran terdaftar, biar nggak generate OTP buat email random
        // (pola sama kayak UserManagementService.requestForgotPassword versi staff).
        customerRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessRuleException("Email tidak ditemukan"));
        otpService.generateAndSend(request.getEmail(), PURPOSE_PASSWORD_RESET);
    }

    public void resetPassword(CustomerResetPasswordRequest request) {
        otpService.verify(request.getEmail(), PURPOSE_PASSWORD_RESET, request.getCode());

        CustomerEntity customer = customerRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessRuleException("Customer tidak ditemukan"));
        customer.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        customerRepository.save(customer);
    }

    public CustomerResponseDTO getOwnProfile(UUID customerId) {
        CustomerEntity customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new BusinessRuleException("Customer tidak ditemukan"));
        CustomerResponseDTO dto = CustomerResponseDTO.from(customer);
        dto.setSisaPlafond(pengajuanService.getSisaPlafond(customer));
        dto.setTierPlafond(userPlafondService.getTierName(customer));
        return dto;
    }

    // Partial update — field null berarti gak diubah, pola sama kayak UpdateUserRequest (staff).
    // Sengaja TIDAK recalculate plafond otomatis di sini walau pendapatan/pekerjaan bisa berubah —
    // UserPlafondService.calculateAndAssign() selalu INSERT baris baru, bukan update in-place,
    // jadi manggil ulang di sini bakal bikin duplikat UserPlafondEntity per customer. Recalculation
    // yang benar butuh method baru (find-or-update), sengaja di luar scope perubahan ini.
    public CustomerResponseDTO updateOwnProfile(UUID customerId, CustomerUpdateRequest request) {
        CustomerEntity customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new BusinessRuleException("Customer tidak ditemukan"));

        if (request.getNamaLengkap() != null) customer.setNamaLengkap(request.getNamaLengkap());

        if (request.getEmail() != null && !request.getEmail().equals(customer.getEmail())) {
            if (customerRepository.existsByEmail(request.getEmail())) {
                throw new BusinessRuleException("Email sudah terdaftar");
            }
            customer.setEmail(request.getEmail());
        }

        if (request.getNoHp() != null && !request.getNoHp().equals(customer.getNoHp())) {
            if (customerRepository.existsByNoHp(request.getNoHp())) {
                throw new BusinessRuleException("Nomor HP sudah terdaftar");
            }
            customer.setNoHp(request.getNoHp());
        }

        if (request.getAlamat() != null) customer.setAlamat(request.getAlamat());
        if (request.getTanggalLahir() != null) customer.setTanggalLahir(request.getTanggalLahir());
        if (request.getTipePekerjaan() != null) customer.setTipePekerjaan(request.getTipePekerjaan());
        if (request.getPekerjaan() != null) customer.setPekerjaan(request.getPekerjaan());
        if (request.getLamaBekerjaBulan() != null) customer.setLamaBekerjaBulan(request.getLamaBekerjaBulan());
        if (request.getPendapatanBulanan() != null) customer.setPendapatanBulanan(request.getPendapatanBulanan());
        if (request.getUtangBerjalan() != null) customer.setUtangBerjalan(request.getUtangBerjalan());

        CustomerEntity saved = customerRepository.save(customer);
        CustomerResponseDTO dto = CustomerResponseDTO.from(saved);
        dto.setSisaPlafond(pengajuanService.getSisaPlafond(saved));
        dto.setTierPlafond(userPlafondService.getTierName(saved));
        return dto;
    }
}