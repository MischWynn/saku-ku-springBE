package com.binar.bc.saku_ku.service;

import com.binar.bc.saku_ku.entity.PengajuanEntity;
import com.binar.bc.saku_ku.entity.ReviewLogEntity;
import com.binar.bc.saku_ku.entity.RoleEntity;
import com.binar.bc.saku_ku.entity.UserEntity;
import com.binar.bc.saku_ku.repository.ReviewLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewLogServiceTest {

    @Mock
    private ReviewLogRepository reviewLogRepository;

    private ReviewLogService service;

    @BeforeEach
    void setUp() {
        service = new ReviewLogService(reviewLogRepository);
    }

    @Test
    void record_savesLogWithSnapshotOfCurrentRole() {
        PengajuanEntity pengajuan = new PengajuanEntity();
        RoleEntity role = new RoleEntity();
        role.setNamaRole("MARKETING");
        UserEntity user = new UserEntity();
        user.setRole(role);

        service.record(pengajuan, user, "MARKETING_APPROVE", "MARKETING_REVIEW", "BM_REVIEW", "OK lanjut");

        ArgumentCaptor<ReviewLogEntity> captor = ArgumentCaptor.forClass(ReviewLogEntity.class);
        verify(reviewLogRepository).save(captor.capture());
        ReviewLogEntity saved = captor.getValue();
        assertThat(saved.getPengajuan()).isSameAs(pengajuan);
        assertThat(saved.getUser()).isSameAs(user);
        assertThat(saved.getRoleName()).isEqualTo("MARKETING");
        assertThat(saved.getAction()).isEqualTo("MARKETING_APPROVE");
        assertThat(saved.getStatusFrom()).isEqualTo("MARKETING_REVIEW");
        assertThat(saved.getStatusTo()).isEqualTo("BM_REVIEW");
        assertThat(saved.getCatatan()).isEqualTo("OK lanjut");
    }

    @Test
    void getHistoryByPengajuan_delegatesToRepository() {
        UUID pengajuanId = UUID.randomUUID();
        ReviewLogEntity log = new ReviewLogEntity();
        when(reviewLogRepository.findByPengajuanIdOrderByCreatedAtAsc(pengajuanId)).thenReturn(List.of(log));

        assertThat(service.getHistoryByPengajuan(pengajuanId)).containsExactly(log);
    }

    @Test
    void getByUser_delegatesToRepository() {
        UUID userId = UUID.randomUUID();
        ReviewLogEntity log = new ReviewLogEntity();
        when(reviewLogRepository.findByUser_IdOrderByCreatedAtDesc(userId)).thenReturn(List.of(log));

        assertThat(service.getByUser(userId)).containsExactly(log);
    }
}
