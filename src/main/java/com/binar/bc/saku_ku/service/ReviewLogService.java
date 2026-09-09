package com.binar.bc.saku_ku.service;

import com.binar.bc.saku_ku.entity.PengajuanEntity;
import com.binar.bc.saku_ku.entity.ReviewLogEntity;
import com.binar.bc.saku_ku.entity.UserEntity;
import com.binar.bc.saku_ku.repository.ReviewLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewLogService {

    private final ReviewLogRepository reviewLogRepository;

    @Transactional
    public void record(
            PengajuanEntity pengajuan,
            UserEntity user,
            String action,
            String statusFrom,
            String statusTo,
            String catatan
    ) {
        ReviewLogEntity log = new ReviewLogEntity();
        log.setPengajuan(pengajuan);
        log.setUser(user);
        log.setRoleName(user.getRole().getNamaRole()); // snapshot role saat ini
        log.setAction(action);
        log.setStatusFrom(statusFrom);
        log.setStatusTo(statusTo);
        log.setCatatan(catatan);

        reviewLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public List<ReviewLogEntity> getHistoryByPengajuan(UUID pengajuanId) {
        return reviewLogRepository.findByPengajuanIdOrderByCreatedAtAsc(pengajuanId);
    }

    @Transactional(readOnly = true)
    public List<ReviewLogEntity> getByUser(UUID userId) {
        return reviewLogRepository.findByUser_IdOrderByCreatedAtDesc(userId);
    }
}