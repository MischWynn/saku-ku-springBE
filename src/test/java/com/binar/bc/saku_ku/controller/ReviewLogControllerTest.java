package com.binar.bc.saku_ku.controller;

import com.binar.bc.saku_ku.entity.ReviewLogEntity;
import com.binar.bc.saku_ku.entity.UserEntity;
import com.binar.bc.saku_ku.exception.UnauthorizedException;
import com.binar.bc.saku_ku.repository.UserRepository;
import com.binar.bc.saku_ku.service.ReviewLogService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewLogControllerTest {

    @Mock
    private ReviewLogService reviewLogService;
    @Mock
    private UserRepository userRepository;

    private ReviewLogController controller;

    @BeforeEach
    void setUp() {
        controller = new ReviewLogController(reviewLogService, userRepository);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getMyHistory_resolvesUserFromJwtSubject() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("dewi.marketing", null));
        UUID userId = UUID.randomUUID();
        UserEntity user = new UserEntity();
        user.setId(userId);
        when(userRepository.findByUsername("dewi.marketing")).thenReturn(Optional.of(user));
        ReviewLogEntity log = new ReviewLogEntity();
        when(reviewLogService.getByUser(userId)).thenReturn(List.of(log));

        assertThat(controller.getMyHistory().getData()).containsExactly(log);
    }

    @Test
    void getMyHistory_throws_whenUserNotFound() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("ghost", null));
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.getMyHistory()).isInstanceOf(UnauthorizedException.class);
    }
}
