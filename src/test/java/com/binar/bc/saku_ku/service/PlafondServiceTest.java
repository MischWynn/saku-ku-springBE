package com.binar.bc.saku_ku.service;

import com.binar.bc.saku_ku.dto.PlafondRequest;
import com.binar.bc.saku_ku.entity.PlafondEntity;
import com.binar.bc.saku_ku.exception.BusinessRuleException;
import com.binar.bc.saku_ku.repository.PlafondRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlafondServiceTest {

    @Mock
    private PlafondRepository plafondRepository;

    private PlafondService service;

    @BeforeEach
    void setUp() {
        service = new PlafondService(plafondRepository);
    }

    @Test
    void getAll_returnsAll() {
        PlafondEntity entity = new PlafondEntity();
        when(plafondRepository.findAll()).thenReturn(List.of(entity));

        assertThat(service.getAll()).containsExactly(entity);
    }

    @Test
    void create_defaultsStatusToActive_whenNotProvided() {
        PlafondRequest request = new PlafondRequest();
        request.setNamaPlafond("Bronze");
        request.setLimitMaksimal(new BigDecimal("5000000"));
        when(plafondRepository.save(any(PlafondEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        PlafondEntity result = service.create(request);

        assertThat(result.getNamaPlafond()).isEqualTo("Bronze");
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void create_usesProvidedStatus_whenPresent() {
        PlafondRequest request = new PlafondRequest();
        request.setStatus("INACTIVE");
        when(plafondRepository.save(any(PlafondEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        PlafondEntity result = service.create(request);

        assertThat(result.getStatus()).isEqualTo("INACTIVE");
    }

    @Test
    void update_onlyUpdatesNonNullFields() {
        UUID id = UUID.randomUUID();
        PlafondEntity existing = PlafondEntity.builder().idPlafond(id).namaPlafond("Old").build();
        when(plafondRepository.findById(id)).thenReturn(Optional.of(existing));
        when(plafondRepository.save(any(PlafondEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        PlafondRequest request = new PlafondRequest();
        request.setNamaPlafond("New");

        PlafondEntity result = service.update(id, request);

        assertThat(result.getNamaPlafond()).isEqualTo("New");
    }

    @Test
    void update_updatesAllFields_whenAllPresent() {
        UUID id = UUID.randomUUID();
        PlafondEntity existing = PlafondEntity.builder().idPlafond(id).build();
        when(plafondRepository.findById(id)).thenReturn(Optional.of(existing));
        when(plafondRepository.save(any(PlafondEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        PlafondRequest request = new PlafondRequest();
        request.setNamaPlafond("Gold");
        request.setDeskripsi("Tier gold");
        request.setTipe("STANDARD");
        request.setLimitMaksimal(new BigDecimal("25000000"));
        request.setStatus("ACTIVE");

        PlafondEntity result = service.update(id, request);

        assertThat(result.getNamaPlafond()).isEqualTo("Gold");
        assertThat(result.getDeskripsi()).isEqualTo("Tier gold");
        assertThat(result.getTipe()).isEqualTo("STANDARD");
        assertThat(result.getLimitMaksimal()).isEqualByComparingTo("25000000");
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void update_throws_whenNotFound() {
        UUID id = UUID.randomUUID();
        when(plafondRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(id, new PlafondRequest())).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void delete_deletes_whenExists() {
        UUID id = UUID.randomUUID();
        when(plafondRepository.existsById(id)).thenReturn(true);

        service.delete(id);

        org.mockito.Mockito.verify(plafondRepository).deleteById(id);
    }

    @Test
    void delete_throws_whenNotExists() {
        UUID id = UUID.randomUUID();
        when(plafondRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(id)).isInstanceOf(BusinessRuleException.class);
    }
}
