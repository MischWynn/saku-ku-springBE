package com.binar.bc.saku_ku.controller;

import com.binar.bc.saku_ku.dto.ApiResponse;
import com.binar.bc.saku_ku.dto.BungaTenorRequest;
import com.binar.bc.saku_ku.entity.BungaTenorEntity;
import com.binar.bc.saku_ku.service.BungaTenorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BungaTenorControllerTest {

    @Mock
    private BungaTenorService bungaTenorService;

    private BungaTenorController controller;

    @BeforeEach
    void setUp() {
        controller = new BungaTenorController(bungaTenorService);
    }

    @Test
    void getAll_wrapsServiceResult() {
        BungaTenorEntity entity = new BungaTenorEntity();
        when(bungaTenorService.getAll()).thenReturn(List.of(entity));

        assertThat(controller.getAll().getData()).containsExactly(entity);
    }

    @Test
    void getById_delegatesToService() {
        UUID id = UUID.randomUUID();
        BungaTenorEntity entity = new BungaTenorEntity();
        when(bungaTenorService.getById(id)).thenReturn(entity);

        assertThat(controller.getById(id).getData()).isSameAs(entity);
    }

    @Test
    void create_delegatesToService() {
        BungaTenorRequest request = new BungaTenorRequest();
        BungaTenorEntity created = new BungaTenorEntity();
        when(bungaTenorService.create(request)).thenReturn(created);

        assertThat(controller.create(request).getData()).isSameAs(created);
    }

    @Test
    void update_delegatesToService() {
        UUID id = UUID.randomUUID();
        BungaTenorRequest request = new BungaTenorRequest();
        BungaTenorEntity updated = new BungaTenorEntity();
        when(bungaTenorService.update(id, request)).thenReturn(updated);

        assertThat(controller.update(id, request).getData()).isSameAs(updated);
    }

    @Test
    void delete_delegatesToService() {
        UUID id = UUID.randomUUID();

        ApiResponse<String> response = controller.delete(id);

        verify(bungaTenorService).delete(id);
        assertThat(response.getStatusCode()).isEqualTo(200);
    }
}
