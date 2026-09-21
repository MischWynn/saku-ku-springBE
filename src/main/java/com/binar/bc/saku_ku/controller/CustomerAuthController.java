package com.binar.bc.saku_ku.controller;

import com.binar.bc.saku_ku.config.swagger.SwaggerExamples;
import com.binar.bc.saku_ku.dto.ApiResponse;
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
import com.binar.bc.saku_ku.service.CustomerAuthService;
import com.binar.bc.saku_ku.service.TokenBlacklistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customer")
@RequiredArgsConstructor
@Tag(name = "Auth - Customer", description = "Register, login, OTP, forgot/reset password, dan profil customer (Android)")
public class CustomerAuthController {

    // 2 varian statis (bukan String.replace() - annotation value HARUS compile-time constant,
    // sebuah method call kayak .replace() gak dianggap konstan meski receiver-nya konstan).
    private static final String CUSTOMER_RESPONSE_EXAMPLE = """
            {"id":"3f5b1a2e-1234-4a3b-9c1d-abcdef123456","namaLengkap":"Novita Sari","nik":"3201010101010001","noHp":"+6281234560001","email":"novita.sari@mail.com","alamat":null,"plafond":12000000,"status":"PENDING_VERIFICATION","tanggalLahir":"1998-05-12","tipePekerjaan":"SWASTA","pekerjaan":"Staff Admin","lamaBekerjaBulan":24,"pendapatanBulanan":5000000,"utangBerjalan":0,"provinsi":"Jawa Barat","kota":"Kota Bandung","kecamatan":"Coblong","namaBank":"BCA","nomorRekening":"1234567890","namaPemilikRekening":"Novita Sari","hasFotoKtp":false,"sisaPlafond":12000000,"tierPlafond":"Bronze"}""";

    private static final String CUSTOMER_RESPONSE_EXAMPLE_ACTIVE = """
            {"id":"3f5b1a2e-1234-4a3b-9c1d-abcdef123456","namaLengkap":"Novita Sari","nik":"3201010101010001","noHp":"+6281234560001","email":"novita.sari@mail.com","alamat":null,"plafond":12000000,"status":"ACTIVE","tanggalLahir":"1998-05-12","tipePekerjaan":"SWASTA","pekerjaan":"Staff Admin","lamaBekerjaBulan":24,"pendapatanBulanan":5000000,"utangBerjalan":0,"provinsi":"Jawa Barat","kota":"Kota Bandung","kecamatan":"Coblong","namaBank":"BCA","nomorRekening":"1234567890","namaPemilikRekening":"Novita Sari","hasFotoKtp":false,"sisaPlafond":12000000,"tierPlafond":"Bronze"}""";

    private final CustomerAuthService customerAuthService;
    private final TokenBlacklistService tokenBlacklistService;

    // POST /api/v1/customer/logout - sama pola kayak staff (/user/logout), lihat
    // TokenBlacklistService buat alasan kenapa clear token lokal doang gak cukup.
    @PostMapping("/logout")
    @Operation(summary = "Logout customer", description = "Blacklist token JWT yang lagi dipakai di Redis (sisa umurnya), bukan cuma clear token lokal.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Logout berhasil", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = "{\"statusCode\":200,\"message\":\"Logout berhasil\",\"data\":null}"))),
            @ApiResponse(responseCode = "401", description = "Token tidak ada/tidak valid", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.UNAUTHORIZED_TOKEN)))
    })
    public ApiResponse<String> logout(HttpServletRequest request) {
        tokenBlacklistService.blacklistFromHeader(request.getHeader(HttpHeaders.AUTHORIZATION));
        return ApiResponse.success(null, "Logout berhasil");
    }

    @PostMapping("/register")
    @SecurityRequirements
    @Operation(summary = "Registrasi customer baru", description = "Akun dibuat dengan status PENDING_VERIFICATION - kirim kode OTP ke email, lanjut ke /verify-otp untuk aktivasi.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registrasi berhasil", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = "{\"statusCode\":200,\"message\":\"Registrasi berhasil, cek email untuk kode OTP verifikasi\",\"data\":" + CUSTOMER_RESPONSE_EXAMPLE + "}"))),
            @ApiResponse(responseCode = "400", description = "Field wajib kosong/format salah", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.BAD_REQUEST))),
            @ApiResponse(responseCode = "422", description = "Email/No HP/NIK sudah terdaftar", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.UNPROCESSABLE_EMAIL_TAKEN)))
    })
    public ApiResponse<CustomerResponseDTO> register(@Valid @RequestBody CustomerRegisterRequest request) {
        CustomerResponseDTO customer = customerAuthService.register(request);
        return ApiResponse.success(customer, "Registrasi berhasil, cek email untuk kode OTP verifikasi");
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Login customer", description = "Identifier boleh email ATAU no HP. Balikin JWT mentah {token,type} - BUKAN dibungkus ApiResponse<>.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login berhasil", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = "{\"token\":\"eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJub3ZpdGEuc2FyaUBtYWlsLmNvbSIsInJvbGUiOiJDVVNUT01FUiJ9.abc123\",\"type\":\"Bearer\"}"))),
            @ApiResponse(responseCode = "400", description = "Identifier/password kosong", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.BAD_REQUEST))),
            @ApiResponse(responseCode = "401", description = "Salah kredensial, atau akun belum ACTIVE (belum verifikasi OTP)", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.UNAUTHORIZED_LOGIN)))
    })
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody CustomerLoginRequest request) {
        return customerAuthService.login(request);
    }

    // Beda dari login() biasa - respons bisa dua bentuk (lihat GoogleSignInResponseDTO.status),
    // makanya dibungkus ApiResponse<> alih-alih AuthResponseDTO mentah kayak login().
    @PostMapping("/google-signin")
    @SecurityRequirements
    @Operation(summary = "Login/cek akun via Google Sign-In", description = "idToken WAJIB Firebase ID token (hasil FirebaseAuth.signInWithCredential di Android), bukan raw Google ID token dari Credential Manager. Kalau email belum terdaftar, balikin NEEDS_REGISTRATION (bukan error) supaya Android bisa lanjut ke Register step 1 dengan email dikunci.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "LOGIN_SUCCESS atau NEEDS_REGISTRATION", content = @Content(
                    mediaType = "application/json",
                    examples = {
                            @ExampleObject(name = "LOGIN_SUCCESS - akun sudah ada", value = "{\"statusCode\":200,\"message\":\"Login berhasil\",\"data\":{\"status\":\"LOGIN_SUCCESS\",\"token\":\"eyJhbGci...\",\"type\":\"Bearer\",\"email\":\"novita.sari@mail.com\",\"namaLengkap\":\"Novita Sari\"}}"),
                            @ExampleObject(name = "NEEDS_REGISTRATION - email belum ada di DB", value = "{\"statusCode\":200,\"message\":\"Akun belum terdaftar, lanjutkan pendaftaran\",\"data\":{\"status\":\"NEEDS_REGISTRATION\",\"token\":null,\"type\":null,\"email\":\"pengguna.baru@gmail.com\",\"namaLengkap\":\"Pengguna Baru\"}}")
                    })),
            @ApiResponse(responseCode = "401", description = "Firebase ID token tidak valid/kedaluwarsa", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = "{\"timestamp\":\"2026-09-21T10:15:30Z\",\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Token Google tidak valid atau sudah kedaluwarsa\"}")))
    })
    public ApiResponse<GoogleSignInResponseDTO> googleSignIn(@Valid @RequestBody GoogleSignInRequest request) {
        GoogleSignInResponseDTO result = customerAuthService.googleSignIn(request);
        String message = GoogleSignInResponseDTO.LOGIN_SUCCESS.equals(result.getStatus())
                ? "Login berhasil"
                : "Akun belum terdaftar, lanjutkan pendaftaran";
        return ApiResponse.success(result, message);
    }

    @PostMapping("/verify-otp")
    @SecurityRequirements
    @Operation(summary = "Verifikasi OTP registrasi", description = "Kode 6 digit, sekali pakai, berlaku 10 menit. Sukses = status akun jadi ACTIVE.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Akun terverifikasi", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = "{\"statusCode\":200,\"message\":\"Akun berhasil diverifikasi\",\"data\":" + CUSTOMER_RESPONSE_EXAMPLE_ACTIVE + "}"))),
            @ApiResponse(responseCode = "400", description = "Kode OTP salah/sudah kedaluwarsa", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = "{\"timestamp\":\"2026-09-21T10:15:30Z\",\"status\":422,\"error\":\"Unprocessable Entity\",\"message\":\"Kode OTP salah atau sudah kadaluarsa\"}")))
    })
    public ApiResponse<CustomerResponseDTO> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        CustomerResponseDTO customer = customerAuthService.verifyRegistrationOtp(request);
        return ApiResponse.success(customer, "Akun berhasil diverifikasi");
    }

    @PostMapping("/resend-otp")
    @SecurityRequirements
    @Operation(summary = "Kirim ulang kode OTP registrasi")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Kode baru terkirim", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = "{\"statusCode\":200,\"message\":\"Kode OTP baru sudah dikirim\",\"data\":null}"))),
            @ApiResponse(responseCode = "400", description = "Email tidak ditemukan / akun sudah terverifikasi", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = "{\"timestamp\":\"2026-09-21T10:15:30Z\",\"status\":422,\"error\":\"Unprocessable Entity\",\"message\":\"Akun sudah terverifikasi\"}")))
    })
    public ApiResponse<String> resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        customerAuthService.resendRegistrationOtp(request);
        return ApiResponse.success(null, "Kode OTP baru sudah dikirim");
    }

    @PostMapping("/forgot-password")
    @SecurityRequirements
    @Operation(summary = "Minta kode OTP reset password", description = "Beda dari change-password (yang perlu login) - ini buat orang yang belum/tidak bisa login.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Kode OTP terkirim ke email", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = "{\"statusCode\":200,\"message\":\"Kode OTP untuk reset password sudah dikirim ke email Anda\",\"data\":null}"))),
            @ApiResponse(responseCode = "400", description = "Email tidak terdaftar", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = "{\"timestamp\":\"2026-09-21T10:15:30Z\",\"status\":422,\"error\":\"Unprocessable Entity\",\"message\":\"Email tidak ditemukan\"}")))
    })
    public ApiResponse<String> forgotPassword(@Valid @RequestBody CustomerForgotPasswordRequest request) {
        customerAuthService.forgotPassword(request);
        return ApiResponse.success(null, "Kode OTP untuk reset password sudah dikirim ke email Anda");
    }

    @PostMapping("/verify-reset-otp")
    @SecurityRequirements
    @Operation(summary = "Cek validitas kode OTP reset password", description = "Cek doang, TIDAK konsumsi kode - konsumsi sebenarnya di /reset-password.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Kode valid", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = "{\"statusCode\":200,\"message\":\"Kode OTP valid\",\"data\":null}"))),
            @ApiResponse(responseCode = "400", description = "Kode salah/kedaluwarsa", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = "{\"timestamp\":\"2026-09-21T10:15:30Z\",\"status\":422,\"error\":\"Unprocessable Entity\",\"message\":\"Kode OTP salah atau sudah kadaluarsa\"}")))
    })
    public ApiResponse<String> verifyResetOtp(@Valid @RequestBody VerifyOtpRequest request) {
        customerAuthService.checkResetPasswordOtp(request);
        return ApiResponse.success(null, "Kode OTP valid");
    }

    @PostMapping("/reset-password")
    @SecurityRequirements
    @Operation(summary = "Reset password pakai kode OTP", description = "Mengonsumsi kode OTP (sekali pakai) - kalau dipanggil dua kali dengan kode yang sama, yang kedua akan gagal.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password berhasil direset", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = "{\"statusCode\":200,\"message\":\"Password berhasil direset\",\"data\":null}"))),
            @ApiResponse(responseCode = "400", description = "Kode OTP salah/kedaluwarsa/sudah dipakai", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = "{\"timestamp\":\"2026-09-21T10:15:30Z\",\"status\":422,\"error\":\"Unprocessable Entity\",\"message\":\"Kode OTP salah atau sudah kadaluarsa\"}")))
    })
    public ApiResponse<String> resetPassword(@Valid @RequestBody CustomerResetPasswordRequest request) {
        customerAuthService.resetPassword(request);
        return ApiResponse.success(null, "Password berhasil direset");
    }

    @PatchMapping("/change-password")
    @Operation(summary = "Ganti password (sudah login)", description = "Beda dari forgot/reset-password (OTP-based) - ini verifikasi password LAMA dulu, pola sesi-terautentikasi standar.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password berhasil diubah", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = "{\"statusCode\":200,\"message\":\"Password berhasil diubah\",\"data\":null}"))),
            @ApiResponse(responseCode = "400", description = "Password lama salah, atau field kosong", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = "{\"timestamp\":\"2026-09-21T10:15:30Z\",\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Password lama salah\"}"))),
            @ApiResponse(responseCode = "401", description = "Belum login", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.UNAUTHORIZED_TOKEN)))
    })
    public ApiResponse<String> changePassword(
            @AuthenticationPrincipal AppCustomerEntity currentCustomer,
            @Valid @RequestBody CustomerChangePasswordRequest request
    ) {
        customerAuthService.changePassword(currentCustomer.getId(), request);
        return ApiResponse.success(null, "Password berhasil diubah");
    }

    @GetMapping("/me")
    @Operation(summary = "Profil customer yang sedang login", description = "sisaPlafond dan tierPlafond dihitung on-the-fly, bukan kolom DB polos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = "{\"statusCode\":200,\"message\":\"OK\",\"data\":" + CUSTOMER_RESPONSE_EXAMPLE_ACTIVE + "}"))),
            @ApiResponse(responseCode = "401", description = "Belum login", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.UNAUTHORIZED_TOKEN)))
    })
    public ApiResponse<CustomerResponseDTO> getOwnProfile(@AuthenticationPrincipal AppCustomerEntity currentCustomer) {
        CustomerResponseDTO customer = customerAuthService.getOwnProfile(currentCustomer.getId());
        return ApiResponse.success(customer, "OK");
    }

    @PatchMapping("/me")
    @Operation(summary = "Update profil sendiri (partial update)", description = "Field null = gak diubah. NIK cuma bisa diisi SEKALI (kalau udah ada, request berikutnya ditolak). Kirim pendapatanBulanan buat trigger recalculate plafond.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profil berhasil diperbarui", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = "{\"statusCode\":200,\"message\":\"Profil berhasil diperbarui\",\"data\":" + CUSTOMER_RESPONSE_EXAMPLE_ACTIVE + "}"))),
            @ApiResponse(responseCode = "400", description = "NIK sudah diisi sebelumnya / email atau No HP sudah dipakai akun lain", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.UNPROCESSABLE_EMAIL_TAKEN))),
            @ApiResponse(responseCode = "401", description = "Belum login", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.UNAUTHORIZED_TOKEN)))
    })
    public ApiResponse<CustomerResponseDTO> updateOwnProfile(
            @AuthenticationPrincipal AppCustomerEntity currentCustomer,
            @Valid @RequestBody CustomerUpdateRequest request
    ) {
        CustomerResponseDTO customer = customerAuthService.updateOwnProfile(currentCustomer.getId(), request);
        return ApiResponse.success(customer, "Profil berhasil diperbarui");
    }

    @PatchMapping("/fcm-token")
    @Operation(summary = "Simpan/update token FCM device", description = "Dipanggil Android abis login sukses / dapet token baru dari sistem (onRegistered) - side-effect device, bukan data profil.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token tersimpan", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = "{\"statusCode\":200,\"message\":\"Token perangkat berhasil disimpan\",\"data\":null}"))),
            @ApiResponse(responseCode = "400", description = "fcmToken kosong", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.BAD_REQUEST)))
    })
    public ApiResponse<String> updateFcmToken(
            @AuthenticationPrincipal AppCustomerEntity currentCustomer,
            @Valid @RequestBody FcmTokenRequest request
    ) {
        customerAuthService.updateFcmToken(currentCustomer.getId(), request);
        return ApiResponse.success(null, "Token perangkat berhasil disimpan");
    }

    @DeleteMapping("/me")
    @Operation(summary = "Hapus akun sendiri (soft-delete)", description = "Wajib konfirmasi password. NIK/email/no HP di-scramble biar bisa dipakai lagi buat registrasi baru - baris DB TETAP ada demi integritas riwayat pengajuan.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Akun berhasil dihapus", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = "{\"statusCode\":200,\"message\":\"Akun berhasil dihapus\",\"data\":null}"))),
            @ApiResponse(responseCode = "400", description = "Password salah, atau akun sudah dihapus sebelumnya", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.UNAUTHORIZED_LOGIN)))
    })
    public ApiResponse<String> deleteOwnAccount(
            @AuthenticationPrincipal AppCustomerEntity currentCustomer,
            @Valid @RequestBody CustomerDeleteAccountRequest request
    ) {
        customerAuthService.deleteOwnAccount(currentCustomer.getId(), request);
        return ApiResponse.success(null, "Akun berhasil dihapus");
    }
}
