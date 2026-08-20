package com.binar.bc.saku_ku.service;

import com.binar.bc.saku_ku.dto.PengajuanRequest;
import com.binar.bc.saku_ku.dto.PengajuanReviewRequest;
import com.binar.bc.saku_ku.entity.BungaTenorEntity;
import com.binar.bc.saku_ku.entity.CustomerEntity;
import com.binar.bc.saku_ku.entity.PengajuanEntity;
import com.binar.bc.saku_ku.exception.BusinessRuleException;
import com.binar.bc.saku_ku.repository.BungaTenorRepository;
import com.binar.bc.saku_ku.repository.CustomerRepository;
import com.binar.bc.saku_ku.repository.PengajuanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PengajuanService {

    private final PengajuanRepository pengajuanRepository;
    private final CustomerRepository customerRepository;
    private final BungaTenorRepository bungaTenorRepository;

    // ==== CREATE (oleh Customer) ====

    @Transactional
    public PengajuanEntity create(UUID currentCustomerId, PengajuanRequest request) {
        CustomerEntity customer = customerRepository.findById(currentCustomerId)
                .orElseThrow(() -> new BusinessRuleException("Customer tidak ditemukan"));

        BungaTenorEntity bungaTenor = bungaTenorRepository.findById(request.getIdBungaTenor())
                .orElseThrow(() -> new BusinessRuleException("Bunga tenor tidak ditemukan"));

        if (!"ACTIVE".equals(bungaTenor.getStatus())) {
            throw new BusinessRuleException("Tenor yang dipilih sedang tidak aktif");
        }

        PengajuanEntity pengajuan = new PengajuanEntity();
        pengajuan.setCustomer(customer);
        pengajuan.setBungaTenor(bungaTenor);
        pengajuan.setNominalPengajuan(request.getNominalPengajuan());
        // snapshot, bukan reference dinamis
        pengajuan.setTenor(bungaTenor.getTenor());
        pengajuan.setInterestRate(bungaTenor.getInterestRate());
        pengajuan.setStatus("MARKETING_REVIEW"); // langsung skip SUBMITTED

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

    // ==== MARKETING actions ====

    @Transactional
    public PengajuanEntity marketingApprove(UUID id, PengajuanReviewRequest request) {
        PengajuanEntity pengajuan = requireStatus(id, "MARKETING_REVIEW");
        pengajuan.setStatus("BM_REVIEW"); // langsung skip MARKETING_APPROVED, auto-lanjut
        return pengajuanRepository.save(pengajuan);
    }

    @Transactional
    public PengajuanEntity marketingReject(UUID id, PengajuanReviewRequest request) {
        PengajuanEntity pengajuan = requireStatus(id, "MARKETING_REVIEW");
        pengajuan.setStatus("MARKETING_REJECTED");
        return pengajuanRepository.save(pengajuan);
    }

    // ==== BM actions ====

    @Transactional
    public PengajuanEntity bmApprove(UUID id, PengajuanReviewRequest request) {
        PengajuanEntity pengajuan = requireStatus(id, "BM_REVIEW");

        if (request.getNominalDisetujui() == null) {
            throw new BusinessRuleException("Nominal disetujui wajib diisi");
        }
        if (request.getNominalDisetujui().compareTo(pengajuan.getNominalPengajuan()) > 0) {
            throw new BusinessRuleException("Nominal disetujui tidak boleh lebih besar dari nominal pengajuan");
        }

        pengajuan.setNominalDisetujui(request.getNominalDisetujui());
        pengajuan.setStatus("BACKOFFICE_REVIEW"); // langsung skip BM_APPROVED, auto-lanjut
        return pengajuanRepository.save(pengajuan);
    }

    @Transactional
    public PengajuanEntity bmReject(UUID id, PengajuanReviewRequest request) {
        PengajuanEntity pengajuan = requireStatus(id, "BM_REVIEW");
        pengajuan.setStatus("BM_REJECTED");
        return pengajuanRepository.save(pengajuan);
    }

    // ==== BACK_OFFICE action ====

    @Transactional
    public PengajuanEntity disburse(UUID id) {
        PengajuanEntity pengajuan = requireStatus(id, "BACKOFFICE_REVIEW");
        pengajuan.setStatus("DISBURSED");
        return pengajuanRepository.save(pengajuan);
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

    // ==== Helper internal ====

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
}