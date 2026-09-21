package com.binar.bc.saku_ku.controller;

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
public class CustomerAuthController {

    private final CustomerAuthService customerAuthService;
    private final TokenBlacklistService tokenBlacklistService;

    // POST /api/v1/customer/logout - sama pola kayak staff (/user/logout), lihat
    // TokenBlacklistService buat alasan kenapa clear token lokal doang gak cukup.
    @PostMapping("/logout")
    public ApiResponse<String> logout(HttpServletRequest request) {
        tokenBlacklistService.blacklistFromHeader(request.getHeader(HttpHeaders.AUTHORIZATION));
        return ApiResponse.success(null, "Logout berhasil");
    }

    @PostMapping("/register")
    public ApiResponse<CustomerResponseDTO> register(@Valid @RequestBody CustomerRegisterRequest request) {
        CustomerResponseDTO customer = customerAuthService.register(request);
        return ApiResponse.success(customer, "Registrasi berhasil, cek email untuk kode OTP verifikasi");
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody CustomerLoginRequest request) {
        return customerAuthService.login(request);
    }

    // Beda dari login() biasa - respons bisa dua bentuk (lihat GoogleSignInResponseDTO.status),
    // makanya dibungkus ApiResponse<> alih-alih AuthResponseDTO mentah kayak login().
    @PostMapping("/google-signin")
    public ApiResponse<GoogleSignInResponseDTO> googleSignIn(@Valid @RequestBody GoogleSignInRequest request) {
        GoogleSignInResponseDTO result = customerAuthService.googleSignIn(request);
        String message = GoogleSignInResponseDTO.LOGIN_SUCCESS.equals(result.getStatus())
                ? "Login berhasil"
                : "Akun belum terdaftar, lanjutkan pendaftaran";
        return ApiResponse.success(result, message);
    }

    @PostMapping("/verify-otp")
    public ApiResponse<CustomerResponseDTO> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        CustomerResponseDTO customer = customerAuthService.verifyRegistrationOtp(request);
        return ApiResponse.success(customer, "Akun berhasil diverifikasi");
    }

    @PostMapping("/resend-otp")
    public ApiResponse<String> resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        customerAuthService.resendRegistrationOtp(request);
        return ApiResponse.success(null, "Kode OTP baru sudah dikirim");
    }

    @PostMapping("/forgot-password")
    public ApiResponse<String> forgotPassword(@Valid @RequestBody CustomerForgotPasswordRequest request) {
        customerAuthService.forgotPassword(request);
        return ApiResponse.success(null, "Kode OTP untuk reset password sudah dikirim ke email Anda");
    }

    @PostMapping("/verify-reset-otp")
    public ApiResponse<String> verifyResetOtp(@Valid @RequestBody VerifyOtpRequest request) {
        customerAuthService.checkResetPasswordOtp(request);
        return ApiResponse.success(null, "Kode OTP valid");
    }

    @PostMapping("/reset-password")
    public ApiResponse<String> resetPassword(@Valid @RequestBody CustomerResetPasswordRequest request) {
        customerAuthService.resetPassword(request);
        return ApiResponse.success(null, "Password berhasil direset");
    }

    @PatchMapping("/change-password")
    public ApiResponse<String> changePassword(
            @AuthenticationPrincipal AppCustomerEntity currentCustomer,
            @Valid @RequestBody CustomerChangePasswordRequest request
    ) {
        customerAuthService.changePassword(currentCustomer.getId(), request);
        return ApiResponse.success(null, "Password berhasil diubah");
    }

    @GetMapping("/me")
    public ApiResponse<CustomerResponseDTO> getOwnProfile(@AuthenticationPrincipal AppCustomerEntity currentCustomer) {
        CustomerResponseDTO customer = customerAuthService.getOwnProfile(currentCustomer.getId());
        return ApiResponse.success(customer, "OK");
    }

    @PatchMapping("/me")
    public ApiResponse<CustomerResponseDTO> updateOwnProfile(
            @AuthenticationPrincipal AppCustomerEntity currentCustomer,
            @Valid @RequestBody CustomerUpdateRequest request
    ) {
        CustomerResponseDTO customer = customerAuthService.updateOwnProfile(currentCustomer.getId(), request);
        return ApiResponse.success(customer, "Profil berhasil diperbarui");
    }

    @PatchMapping("/fcm-token")
    public ApiResponse<String> updateFcmToken(
            @AuthenticationPrincipal AppCustomerEntity currentCustomer,
            @Valid @RequestBody FcmTokenRequest request
    ) {
        customerAuthService.updateFcmToken(currentCustomer.getId(), request);
        return ApiResponse.success(null, "Token perangkat berhasil disimpan");
    }

    @DeleteMapping("/me")
    public ApiResponse<String> deleteOwnAccount(
            @AuthenticationPrincipal AppCustomerEntity currentCustomer,
            @Valid @RequestBody CustomerDeleteAccountRequest request
    ) {
        customerAuthService.deleteOwnAccount(currentCustomer.getId(), request);
        return ApiResponse.success(null, "Akun berhasil dihapus");
    }
}