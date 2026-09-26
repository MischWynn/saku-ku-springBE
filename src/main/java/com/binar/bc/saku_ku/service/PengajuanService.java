package com.binar.bc.saku_ku.service;

import com.binar.bc.saku_ku.util.AgePolicy;
import com.binar.bc.saku_ku.dto.PengajuanRequest;
import com.binar.bc.saku_ku.dto.PengajuanReviewRequest;
import com.binar.bc.saku_ku.entity.BungaTenorEntity;
import com.binar.bc.saku_ku.entity.CustomerEntity;
import com.binar.bc.saku_ku.entity.PengajuanEntity;
import com.binar.bc.saku_ku.entity.UserEntity;
import com.binar.bc.saku_ku.entity.UserPlafondEntity;
import com.binar.bc.saku_ku.exception.BusinessRuleException;
import com.binar.bc.saku_ku.repository.BungaTenorRepository;
import com.binar.bc.saku_ku.repository.CustomerRepository;
import com.binar.bc.saku_ku.repository.PengajuanRepository;
import com.binar.bc.saku_ku.repository.UserPlafondRepository;
import com.binar.bc.saku_ku.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PengajuanService {

    private final PengajuanRepository pengajuanRepository;
    private final CustomerRepository customerRepository;
    private final BungaTenorRepository bungaTenorRepository;
    private final ReviewLogService reviewLogService;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final UserPlafondRepository userPlafondRepository;

    // ==== CREATE (oleh Customer) ====

    @Transactional
    public PengajuanEntity create(UUID currentCustomerId, PengajuanRequest request) {
        CustomerEntity customer = customerRepository.findById(currentCustomerId)
                .orElseThrow(() -> new BusinessRuleException("Customer tidak ditemukan"));

        // pekerjaan/pendapatanBulanan sengaja OPSIONAL pas registrasi (Step 3 Android cuma
        // wajibin tipePekerjaan, dua field ini bisa dikosongin) - tapi keduanya WAJIB buat
        // ngajuin pinjaman beneran, soalnya jadi basis formula plafond (UserPlafondService) dan
        // DBR yang dilihat staff pas review. Tanpa ini, customer bisa ngajuin dengan plafond
        // yang masih kejebak di fallback minimum dan data Employment & Financial kosong di
        // drawer staff.
        if (customer.getPekerjaan() == null || customer.getPekerjaan().isBlank()
                || customer.getPendapatanBulanan() == null) {
            throw new BusinessRuleException("Lengkapi data pekerjaan dan pendapatan bulanan di profil Anda sebelum mengajukan pinjaman");
        }
        // Tanggal lahir juga bagian dari profil lengkap, dan nasabah harus minimal 17 tahun
        // (syarat KTP) - sama kayak gerbang di layar Ajukan Pinjaman Android.
        if (customer.getTanggalLahir() == null) {
            throw new BusinessRuleException("Lengkapi tanggal lahir di profil Anda sebelum mengajukan pinjaman");
        }
        if (!AgePolicy.isOldEnough(customer.getTanggalLahir())) {
            throw new BusinessRuleException("Pengajuan pinjaman hanya untuk nasabah berusia minimal " + AgePolicy.MIN_AGE + " tahun");
        }
        // Rekening tujuan pencairan wajib ada - dana yang disetujui dicairin ke sini.
        if (isBlank(customer.getNamaBank()) || isBlank(customer.getNomorRekening())
                || isBlank(customer.getNamaPemilikRekening())) {
            throw new BusinessRuleException("Lengkapi data rekening bank di profil Anda sebelum mengajukan pinjaman");
        }
        // NIK + foto KTP wajib - staff butuh keduanya buat verifikasi identitas di drawer review.
        // Bisa kosong kalau customer ngelewatin langkah Identitas pas registrasi (auto-login
        // udah jalan abis OTP), diisi lewat Profil -> KTP & Data Diri.
        if (isBlank(customer.getNik()) || isBlank(customer.getFotoKtp())) {
            throw new BusinessRuleException("Lengkapi NIK dan foto KTP di profil Anda sebelum mengajukan pinjaman");
        }

        BungaTenorEntity bungaTenor = bungaTenorRepository.findById(request.getIdBungaTenor())
                .orElseThrow(() -> new BusinessRuleException("Bunga tenor tidak ditemukan"));

        if (!"ACTIVE".equals(bungaTenor.getStatus())) {
            throw new BusinessRuleException("Tenor yang dipilih sedang tidak aktif");
        }

        BigDecimal effectiveLimit = getEffectiveLimit(customer);
        if (effectiveLimit != null) {
            BigDecimal heldNominal = pengajuanRepository.sumHeldNominalByCustomer(customer.getId());
            BigDecimal sisaPlafond = effectiveLimit.subtract(heldNominal).max(BigDecimal.ZERO);
            if (request.getNominalPengajuan().compareTo(sisaPlafond) > 0) {
                throw new BusinessRuleException(
                        "Nominal pengajuan melebihi sisa plafond Anda (sisa Rp " + sisaPlafond
                                + " dari limit Rp " + effectiveLimit + ", sedang terpakai Rp " + heldNominal + ")");
            }
        }

        PengajuanEntity pengajuan = new PengajuanEntity();
        pengajuan.setCustomer(customer);
        pengajuan.setBungaTenor(bungaTenor);
        pengajuan.setNominalPengajuan(request.getNominalPengajuan());
        pengajuan.setTenor(bungaTenor.getTenor());
        pengajuan.setInterestRate(bungaTenor.getInterestRate());
        pengajuan.setTujuanPinjaman(request.getTujuanPinjaman());
        pengajuan.setCreatedAt(LocalDateTime.now());
        pengajuan.setUpdatedAt(LocalDateTime.now());
        pengajuan.setStatus("MARKETING_REVIEW");

        return pengajuanRepository.save(pengajuan);
    }

    // ==== READ ====

    public PengajuanEntity getById(UUID id) {
        return pengajuanRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("Pengajuan tidak ditemukan"));
    }

    public List<PengajuanEntity> getByCustomer(UUID customerId) {
        return pengajuanRepository.findByCustomerId(customerId);
    }

    public List<PengajuanEntity> getByStatus(String status) {
        return pengajuanRepository.findByStatus(status);
    }

    public List<PengajuanEntity> getAll() {
        return pengajuanRepository.findAll();
    }


    @Transactional
    public PengajuanEntity marketingApprove(UUID id, String currentUsername, PengajuanReviewRequest request) {
        PengajuanEntity pengajuan = requireStatus(id, "MARKETING_REVIEW");
        String statusFrom = pengajuan.getStatus();

        pengajuan.setStatus("BM_REVIEW");
        PengajuanEntity saved = pengajuanRepository.save(pengajuan);

        UserEntity user = getStaffUser(currentUsername);
        reviewLogService.record(saved, user, "APPROVE", statusFrom, saved.getStatus(), request.getCatatan());
        notificationService.create(saved.getCustomer(), saved, 
        "Pengajuan Disetujui", "Pengajuan Anda telah disetujui oleh Marketing dan diteruskan ke BM untuk ditinjau.");

        return saved;
    }

    @Transactional
    public PengajuanEntity marketingReject(UUID id, String currentUsername, PengajuanReviewRequest request) {
        PengajuanEntity pengajuan = requireStatus(id, "MARKETING_REVIEW");
        String statusFrom = pengajuan.getStatus();

        pengajuan.setStatus("MARKETING_REJECTED");
        PengajuanEntity saved = pengajuanRepository.save(pengajuan);

        UserEntity user = getStaffUser(currentUsername);
        reviewLogService.record(saved, user, "REJECT", statusFrom, saved.getStatus(), request.getCatatan());
        notificationService.create(saved.getCustomer(), saved,
        "Pengajuan Ditolak", "Pengajuan Anda telah ditolak oleh Marketing. Silakan periksa catatan untuk informasi lebih lanjut.");

        return saved;
    }

    // ==== BM actions ====

    @Transactional
    public PengajuanEntity bmApprove(UUID id, String currentUsername, PengajuanReviewRequest request) {
        PengajuanEntity pengajuan = requireStatus(id, "BM_REVIEW");
        String statusFrom = pengajuan.getStatus();

        if (request.getNominalDisetujui() == null) {
            throw new BusinessRuleException("Nominal disetujui wajib diisi");
        }
        if (request.getNominalDisetujui().compareTo(pengajuan.getNominalPengajuan()) > 0) {
            throw new BusinessRuleException("Nominal disetujui tidak boleh lebih besar dari nominal pengajuan");
        }

        pengajuan.setNominalDisetujui(request.getNominalDisetujui());
        pengajuan.setStatus("BACKOFFICE_REVIEW");
        PengajuanEntity saved = pengajuanRepository.save(pengajuan);

        UserEntity user = getStaffUser(currentUsername);
        reviewLogService.record(saved, user, "APPROVE", statusFrom, saved.getStatus(), request.getCatatan());
        notificationService.create(saved.getCustomer(), saved,
        "Pengajuan Disetujui oleh Branch Manager", "Pengajuan Anda telah disetujui oleh BM dan diteruskan ke Back Office untuk diproses pencairan.");

        return saved;
    }

    @Transactional
    public PengajuanEntity bmReject(UUID id, String currentUsername, PengajuanReviewRequest request) {
        PengajuanEntity pengajuan = requireStatus(id, "BM_REVIEW");
        String statusFrom = pengajuan.getStatus();

        pengajuan.setStatus("BM_REJECTED");
        PengajuanEntity saved = pengajuanRepository.save(pengajuan);

        UserEntity user = getStaffUser(currentUsername);
        reviewLogService.record(saved, user, "REJECT", statusFrom, saved.getStatus(), request.getCatatan());
        notificationService.create(saved.getCustomer(), saved,
        "Pengajuan Ditolak oleh Branch Manager", "Pengajuan anda telah ditolak oleh Branch Manager. Pengajuan tidak dapat disetujui. Silakan periksa catatan untuk informasi lebih lanjut.");

        return saved;
    }


    // ==== BACK_OFFICE action ====

    @Transactional
    public PengajuanEntity disburse(UUID id, String currentUsername) {
        PengajuanEntity pengajuan = requireStatus(id, "BACKOFFICE_REVIEW");
        String statusFrom = pengajuan.getStatus();

        pengajuan.setStatus("DISBURSED");
        pengajuan.setTanggalPencairan(LocalDateTime.now());
        PengajuanEntity saved = pengajuanRepository.save(pengajuan);

        UserEntity user = getStaffUser(currentUsername);
        reviewLogService.record(saved, user, "DISBURSE", statusFrom, saved.getStatus(), null);
        notificationService.create(saved.getCustomer(), saved,
        "Dana Pengajuan berhasil dicairkan!", "Pengajuan Anda telah dicairkan oleh Back Office. Silakan cek rekening Anda untuk memastikan dana telah diterima.");
        return saved;
    }
    // ==== CANCEL ====

    @Transactional
    public PengajuanEntity cancelByCustomer(UUID id, UUID currentCustomerId) {
        PengajuanEntity pengajuan = getById(id);

        if (!pengajuan.getCustomer().getId().equals(currentCustomerId)) {
            throw new BusinessRuleException("Anda tidak berhak membatalkan pengajuan ini");
        }
        if (!isCancellableByCustomer(pengajuan.getStatus())) {
            throw new BusinessRuleException("Pengajuan tidak bisa dibatalkan pada status saat ini");
        }

        pengajuan.setStatus("CANCELLED");
        return pengajuanRepository.save(pengajuan);
    }

    @Transactional
    public PengajuanEntity cancelBySuperadmin(UUID id) {
        PengajuanEntity pengajuan = getById(id);

        if ("DISBURSED".equals(pengajuan.getStatus())) {
            throw new BusinessRuleException("Pengajuan yang sudah dicairkan tidak bisa dibatalkan");
        }
        if (isTerminalStatus(pengajuan.getStatus())) {
            throw new BusinessRuleException("Pengajuan sudah dalam status akhir: " + pengajuan.getStatus());
        }

        pengajuan.setStatus("CANCELLED");
        return pengajuanRepository.save(pengajuan);
    }

    // ==== Sisa plafond (dipakai juga oleh CustomerAuthService buat customer/me) ====

    public BigDecimal getEffectiveLimit(CustomerEntity customer) {
        return userPlafondRepository.findByCustomer_Id(customer.getId())
                .map(UserPlafondEntity::getLimitEfektif)
                .orElse(customer.getPlafond());
    }

    public BigDecimal getSisaPlafond(CustomerEntity customer) {
        BigDecimal effectiveLimit = getEffectiveLimit(customer);
        if (effectiveLimit == null) {
            return null;
        }
        BigDecimal heldNominal = pengajuanRepository.sumHeldNominalByCustomer(customer.getId());
        return effectiveLimit.subtract(heldNominal).max(BigDecimal.ZERO);
    }

    // ==== Kunci foto KTP ====

    private static final List<String> IN_REVIEW_STATUSES = List.of("MARKETING_REVIEW", "BM_REVIEW", "BACKOFFICE_REVIEW");

    /**
     * null = customer boleh (ganti) foto KTP. Selain itu alasan kenapa terkunci:
     * - pernah DISBURSED: permanen - foto itu jadi bukti identitas pas pinjaman disetujui.
     * - ada pengajuan lagi direview: sementara - biar foto yang dilihat staff gak ganti di tengah proses.
     * Pengajuan yang ditolak/dibatalkan gak ngunci (customer boleh foto ulang lalu ajukan lagi).
     */
    public String getFotoKtpLockReason(CustomerEntity customer) {
        if (pengajuanRepository.existsByCustomerIdAndStatusIn(customer.getId(), List.of("DISBURSED"))) {
            return "Foto KTP terkunci karena kamu sudah pernah menerima pencairan dana. Hubungi CS kalau perlu mengganti.";
        }
        if (pengajuanRepository.existsByCustomerIdAndStatusIn(customer.getId(), IN_REVIEW_STATUSES)) {
            return "Foto KTP tidak bisa diganti selama pengajuanmu sedang direview.";
        }
        return null;
    }

    // ==== Helper internal ====

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private PengajuanEntity requireStatus(UUID id, String expectedStatus) {
        PengajuanEntity pengajuan = getById(id);
        if (!expectedStatus.equals(pengajuan.getStatus())) {
            throw new BusinessRuleException(
                    "Pengajuan harus berstatus " + expectedStatus + ", saat ini: " + pengajuan.getStatus());
        }
        return pengajuan;
    }

    private boolean isCancellableByCustomer(String status) {
        return "MARKETING_REVIEW".equals(status) || "BM_REVIEW".equals(status);
    }

    private boolean isTerminalStatus(String status) {
        return "MARKETING_REJECTED".equals(status)
                || "BM_REJECTED".equals(status)
                || "DISBURSED".equals(status)
                || "CANCELLED".equals(status);
    }

    private UserEntity getStaffUser(String username) {
        return userRepository.findByUsernameAndDeletedDateIsNull(username)
                .orElseThrow(() -> new BusinessRuleException("Staff tidak ditemukan"));
    }

    

}