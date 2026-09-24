package com.binar.bc.saku_ku.service;

import com.binar.bc.saku_ku.dto.BungaTenorRequest;
import com.binar.bc.saku_ku.entity.BungaTenorEntity;
import com.binar.bc.saku_ku.exception.BusinessRuleException;
import com.binar.bc.saku_ku.repository.BungaTenorRepository;
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
class BungaTenorServiceTest {

    @Mock
    private BungaTenorRepository bungaTenorRepository;

    private BungaTenorService service;

    @BeforeEach
    void setUp() {
        service = new BungaTenorService(bungaTenorRepository);
    }

    @Test
    void getAll_returnsAll() {
        BungaTenorEntity entity = new BungaTenorEntity();
        when(bungaTenorRepository.findAll()).thenReturn(List.of(entity));

        assertThat(service.getAll()).containsExactly(entity);
    }

    @Test
    void getById_returnsEntity_whenFound() {
        UUID id = UUID.randomUUID();
        BungaTenorEntity entity = new BungaTenorEntity();
        entity.setId(id);
        when(bungaTenorRepository.findById(id)).thenReturn(Optional.of(entity));

        assertThat(service.getById(id)).isSameAs(entity);
    }

    @Test
    void getById_throws_whenNotFound() {
        UUID id = UUID.randomUUID();
        when(bungaTenorRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(id)).isInstanceOf(RuntimeException.class);
    }

    @Test
    void create_defaultsStatusToActive() {
        BungaTenorRequest request = new BungaTenorRequest();
        request.setTenor(12);
        request.setInterestRate(new BigDecimal("3.0"));
        when(bungaTenorRepository.existsByTenor(12)).thenReturn(false);
        when(bungaTenorRepository.save(any(BungaTenorEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        BungaTenorEntity result = service.create(request);

        assertThat(result.getTenor()).isEqualTo(12);
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void create_throws_whenTenorAlreadyExists() {
        BungaTenorRequest request = new BungaTenorRequest();
        request.setTenor(12);
        when(bungaTenorRepository.existsByTenor(12)).thenReturn(true);

        assertThatThrownBy(() -> service.create(request)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void update_onlyUpdatesNonNullFields() {
        UUID id = UUID.randomUUID();
        BungaTenorEntity existing = new BungaTenorEntity();
        existing.setId(id);
        existing.setTenor(6);
        when(bungaTenorRepository.findById(id)).thenReturn(Optional.of(existing));
        when(bungaTenorRepository.save(any(BungaTenorEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        BungaTenorRequest request = new BungaTenorRequest();
        request.setInterestRate(new BigDecimal("2.5"));

        BungaTenorEntity result = service.update(id, request);

        assertThat(result.getTenor()).isEqualTo(6);
        assertThat(result.getInterestRate()).isEqualByComparingTo("2.5");
    }

    @Test
    void update_throws_whenNotFound() {
        UUID id = UUID.randomUUID();
        when(bungaTenorRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(id, new BungaTenorRequest())).isInstanceOf(RuntimeException.class);
    }

    @Test
    void delete_deletes_whenFound() {
        UUID id = UUID.randomUUID();
        BungaTenorEntity existing = new BungaTenorEntity();
        existing.setId(id);
        when(bungaTenorRepository.findById(id)).thenReturn(Optional.of(existing));

        service.delete(id);

        org.mockito.Mockito.verify(bungaTenorRepository).delete(existing);
    }

    @Test
    void delete_throws_whenNotFound() {
        UUID id = UUID.randomUUID();
        when(bungaTenorRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(id)).isInstanceOf(RuntimeException.class);
    }
}
