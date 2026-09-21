package com.binar.bc.saku_ku.service;

import com.binar.bc.saku_ku.dto.AuthResponseDTO;
import com.binar.bc.saku_ku.dto.CustomerChangePasswordRequest;
import com.binar.bc.saku_ku.dto.CustomerDeleteAccountRequest;
import com.binar.bc.saku_ku.dto.CustomerForgotPasswordRequest;
import com.binar.bc.saku_ku.dto.CustomerLoginRequest;
import com.binar.bc.saku_ku.dto.CustomerRegisterRequest;
import com.binar.bc.saku_ku.dto.CustomerResetPasswordRequest;
import com.binar.bc.saku_ku.dto.CustomerResponseDTO;
import com.binar.bc.saku_ku.dto.CustomerUpdateRequest;
import com.binar.bc.saku_ku.dto.FcmTokenRequest;
import com.binar.bc.saku_ku.dto.GoogleSignInRequest;
import com.binar.bc.saku_ku.dto.GoogleSignInResponseDTO;
import com.binar.bc.saku_ku.dto.ResendOtpRequest;
import com.binar.bc.saku_ku.dto.VerifyOtpRequest;
import com.binar.bc.saku_ku.entity.AppCustomerEntity;
import com.binar.bc.saku_ku.entity.CustomerEntity;
import com.binar.bc.saku_ku.exception.BusinessRuleException;
import com.binar.bc.saku_ku.exception.UnauthorizedException;
import com.binar.bc.saku_ku.repository.CustomerRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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
        if (request.getNik() != null && customerRepository.existsByNik(request.getNik())) {
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
        customer.setStatus("PENDING_VERIFICATION");
        customer.setTanggalLahir(request.getTanggalLahir());
        customer.setTipePekerjaan(request.getTipePekerjaan());
        customer.setPekerjaan(request.getPekerjaan());
        customer.setLamaBekerjaBulan(request.getLamaBekerjaBulan());
        customer.setPendapatanBulanan(request.getPendapatanBulanan());
        customer.setUtangBerjalan(request.getUtangBerjalan());
        customer.setProvinsi(request.getProvinsi());
        customer.setKota(request.getKota());
        customer.setKecamatan(request.getKecamatan());
        customer.setNamaBank(request.getNamaBank());
        customer.setNomorRekening(request.getNomorRekening());
        customer.setNamaPemilikRekening(request.getNamaPemilikRekening());

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
        if (fullCustomer.getDeletedDate() != null) {
            throw new UnauthorizedException("Email/No HP atau password salah");
        }
        if (!"ACTIVE".equals(fullCustomer.getStatus())) {
            throw new UnauthorizedException("Akun belum diverifikasi. Cek email Anda untuk kode OTP, atau minta kode baru.");
        }

        String token = jwtService.issue(customer.getUsername(), customer.getRole(), Instant.now());
        return ResponseEntity.ok(new AuthResponseDTO(token));
    }

    //Ini buat id token dari google sign in, nanti di android dikirim ke backend buat di verifikasi pakai firebase admin sdk. Kalau valid, backend bikin JWT baru buat customer ini dan dikirim balik ke android.
    // Kalau email belum ada di database, backend kirim balik response "needs registration" biasanya Android langsung arahkan user ke Register step 1 (email dikunci, prefilled dari sini).
    public GoogleSignInResponseDTO googleSignIn(GoogleSignInRequest request) {
        FirebaseToken decoded;
        try {
            decoded = FirebaseAuth.getInstance().verifyIdToken(request.getIdToken());
        } catch (FirebaseAuthException e) {
            log.warn("Google sign-in ditolak - token tidak valid: {}", e.getMessage());
            throw new UnauthorizedException("Token Google tidak valid atau sudah kedaluwarsa");
        }

        String email = decoded.getEmail();
        if (email == null || email.isBlank()) {
            throw new BusinessRuleException("Akun Google tidak memiliki email terverifikasi");
        }
        String name = decoded.getName();

        Optional<CustomerEntity> existing = customerRepository.findByEmail(email);
        if (existing.isEmpty()) {
            return GoogleSignInResponseDTO.needsRegistration(email, name);
        }

        CustomerEntity customer = existing.get();
        if (customer.getDeletedDate() != null || !"ACTIVE".equals(customer.getStatus())) {
            throw new UnauthorizedException("Akun belum aktif. Cek email Anda untuk kode OTP, atau minta kode baru.");
        }

        AppCustomerEntity appCustomer = appCustomerDetailsService.findCustomer(email)
                .orElseThrow(() -> new UnauthorizedException("Akun tidak ditemukan"));
        String token = jwtService.issue(appCustomer.getUsername(), appCustomer.getRole(), Instant.now());
        return GoogleSignInResponseDTO.loggedIn(token, email, customer.getNamaLengkap());
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

    // OTP-based password reset flow: Android kirim email, backend generate OTP & kirim ke email itu. Android kirim balik email+OTP+newPassword, backend verifikasi OTP dan update password. Pola sama kayak UserManagementService.requestForgotPassword() + resetPassword() punya staff.
    public void checkResetPasswordOtp(VerifyOtpRequest request) {
        boolean valid = otpService.isValid(request.getEmail(), PURPOSE_PASSWORD_RESET, request.getCode());
        if (!valid) {
            throw new BusinessRuleException("Kode OTP salah atau sudah kadaluarsa");
        }
    }

    // dicek dulu emailnya ada apa engga di customerrepository, kalau ada generate OTP baru dan kirim ke email itu. Kalau emailnya ga ada, throw exception "Email tidak ditemukan" (bukan silent fail) biar Android bisa kasih feedback ke user.
    public void forgotPassword(CustomerForgotPasswordRequest request) {
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

    // ini ubah password, tapi harus login dulu (ada JWT) biar bisa ubah password sendiri. Kalau lupa password, pakai forgotPassword() + resetPassword() di atas.
    public void changePassword(UUID customerId, CustomerChangePasswordRequest request) {
        CustomerEntity customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new BusinessRuleException("Customer tidak ditemukan"));

        if (!passwordEncoder.matches(request.getOldPassword(), customer.getPasswordHash())) {
            throw new UnauthorizedException("Password lama salah");
        }

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


    // ini update profil tidak semuanya pakai patch. tidak ada recalculate otomatis disini walau pekerjaannya berubah. 
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

        if (request.getNik() != null) {
            if (customer.getNik() != null) {
                throw new BusinessRuleException("NIK sudah diisi dan tidak bisa diubah");
            }
            if (customerRepository.existsByNik(request.getNik())) {
                throw new BusinessRuleException("NIK sudah terdaftar");
            }
            customer.setNik(request.getNik());
        }

        if (request.getAlamat() != null) customer.setAlamat(request.getAlamat());
        if (request.getTanggalLahir() != null) customer.setTanggalLahir(request.getTanggalLahir());
        if (request.getTipePekerjaan() != null) customer.setTipePekerjaan(request.getTipePekerjaan());
        if (request.getPekerjaan() != null) customer.setPekerjaan(request.getPekerjaan());
        if (request.getLamaBekerjaBulan() != null) customer.setLamaBekerjaBulan(request.getLamaBekerjaBulan());
        if (request.getPendapatanBulanan() != null) customer.setPendapatanBulanan(request.getPendapatanBulanan());
        if (request.getUtangBerjalan() != null) customer.setUtangBerjalan(request.getUtangBerjalan());

        // Data pekerjaan (untuk pendapatan) dipidnahin ke step terakhir di register() biar Android bisa kirim data pekerjaan belakangan (step 3) tanpa bikin plafon salah hitung. Jadi kalau user update profil di step 1/2, plafon TIDAK dihitung ulang di sini. Kalau user update data pekerjaan (step 3), plafon dihitung ulang di titik itu (userPlafondService.recalculate()).
        if (request.getPendapatanBulanan() != null) {
            userPlafondService.recalculate(customer);
        }
        if (request.getFotoKtp() != null) customer.setFotoKtp(request.getFotoKtp());
        if (request.getProvinsi() != null) customer.setProvinsi(request.getProvinsi());
        if (request.getKota() != null) customer.setKota(request.getKota());
        if (request.getKecamatan() != null) customer.setKecamatan(request.getKecamatan());
        if (request.getNamaBank() != null) customer.setNamaBank(request.getNamaBank());
        if (request.getNomorRekening() != null) customer.setNomorRekening(request.getNomorRekening());
        if (request.getNamaPemilikRekening() != null) customer.setNamaPemilikRekening(request.getNamaPemilikRekening());

        CustomerEntity saved = customerRepository.save(customer);
        CustomerResponseDTO dto = CustomerResponseDTO.from(saved);
        dto.setSisaPlafond(pengajuanService.getSisaPlafond(saved));
        dto.setTierPlafond(userPlafondService.getTierName(saved));
        return dto;
    }


    // Self-service delete, Android Profil -> "Hapus Akun". Soft-delete (deletedDate), row TETAP ada di DB (riwayat pengajuan customer ini gak boleh ikut rusak/kehilangan referensi) - tapi nik/email/no_hp di-scramble biar slot unique constraint-nya kebebasin, customer boleh daftar ulang pakai NIK/email yang sama kapan aja setelah ini, sesuai keputusan user.
    public void deleteOwnAccount(UUID customerId, CustomerDeleteAccountRequest request) {
        CustomerEntity customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new BusinessRuleException("Customer tidak ditemukan"));

        if (!passwordEncoder.matches(request.getPassword(), customer.getPasswordHash())) {
            throw new UnauthorizedException("Password salah");
        }
        if (customer.getDeletedDate() != null) {
            throw new BusinessRuleException("Akun sudah dihapus");
        }

        // Turunan dari UUID customer sendiri - deterministik & dijamin gak collide sama customer lain yang juga dihapus, dipotong sesuai panjang kolom masing-masing (nik VARCHAR(16), no_hp VARCHAR(20)).
        String hex = customer.getId().toString().replace("-", "");
        customer.setNik(("DEL" + hex).substring(0, 16));
        customer.setNoHp(("DEL" + hex).substring(0, 20));
        customer.setEmail("del-" + hex + "@deleted.sakuku.local");
        customer.setDeletedDate(java.time.LocalDateTime.now());

        customerRepository.save(customer);
    }

    // ini dipakai untuk update FCM token di database, biar backend bisa push notification ke device user. Dipanggil Android abis login sukses / dapet FCM token baru dari sistem (onRegistered). Endpoint terpisah dari PATCH customer/me sengaja - ini side-effect device, bukan data profil yang diedit user, jadi Android bisa panggil ini diem-diem di background tanpa lewat form apa pun. Overwrite polos (bukan partial-update dgn null-check) - token lama otomatis kegantiin kalau install ulang/ganti device.
    public void updateFcmToken(UUID customerId, FcmTokenRequest request) {
        CustomerEntity customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new BusinessRuleException("Customer tidak ditemukan"));
        customer.setFcmToken(request.getFcmToken());
        customerRepository.save(customer);
    }
}