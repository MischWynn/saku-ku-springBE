package com.binar.bc.saku_ku.controller;

import com.binar.bc.saku_ku.dto.ApiResponse;
import com.binar.bc.saku_ku.dto.PlafondRequest;
import com.binar.bc.saku_ku.entity.PlafondEntity;
import com.binar.bc.saku_ku.service.PlafondService;
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
class PlafondControllerTest {

    @Mock
    private PlafondService plafondService;

    private PlafondController controller;

    @BeforeEach
    void setUp() {
        controller = new PlafondController(plafondService);
    }

    @Test
    void getAll_wrapsServiceResult() {
        PlafondEntity entity = new PlafondEntity();
        when(plafondService.getAll()).thenReturn(List.of(entity));

        assertThat(controller.getAll().getData()).containsExactly(entity);
    }

    @Test
    void create_delegatesToService() {
        PlafondRequest request = new PlafondRequest();
        PlafondEntity created = new PlafondEntity();
        when(plafondService.create(request)).thenReturn(created);

        assertThat(controller.create(request).getData()).isSameAs(created);
    }

    @Test
    void update_delegatesToService() {
        UUID id = UUID.randomUUID();
        PlafondRequest request = new PlafondRequest();
        PlafondEntity updated = new PlafondEntity();
        when(plafondService.update(id, request)).thenReturn(updated);

        assertThat(controller.update(id, request).getData()).isSameAs(updated);
    }

    @Test
    void delete_delegatesToService() {
        UUID id = UUID.randomUUID();

        ApiResponse<String> response = controller.delete(id);

        verify(plafondService).delete(id);
        assertThat(response.getStatusCode()).isEqualTo(200);
    }
}
