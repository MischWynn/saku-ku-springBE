package com.binar.bc.saku_ku.service;

import com.binar.bc.saku_ku.entity.AppCustomerEntity;
import com.binar.bc.saku_ku.entity.CustomerEntity;
import com.binar.bc.saku_ku.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppCustomerDetailsServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    private AppCustomerDetailsService service;

    @BeforeEach
    void setUp() {
        service = new AppCustomerDetailsService(customerRepository);
    }

    private CustomerEntity customer() {
        CustomerEntity customer = new CustomerEntity();
        customer.setId(UUID.randomUUID());
        customer.setEmail("nasabah@mail.com");
        customer.setPasswordHash("hashed");
        return customer;
    }

    @Test
    void findCustomer_findsByEmail_whenMatches() {
        CustomerEntity customer = customer();
        when(customerRepository.findByEmail("nasabah@mail.com")).thenReturn(Optional.of(customer));

        Optional<AppCustomerEntity> result = service.findCustomer("nasabah@mail.com");

        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("nasabah@mail.com");
        assertThat(result.get().getPassword()).isEqualTo("hashed");
    }

    @Test
    void findCustomer_fallsBackToNoHp_whenEmailNotFound() {
        CustomerEntity customer = customer();
        when(customerRepository.findByEmail("08123456789")).thenReturn(Optional.empty());
        when(customerRepository.findByNoHp("08123456789")).thenReturn(Optional.of(customer));

        Optional<AppCustomerEntity> result = service.findCustomer("08123456789");

        assertThat(result).isPresent();
    }

    @Test
    void findCustomer_returnsEmpty_whenNeitherMatches() {
        when(customerRepository.findByEmail("ghost")).thenReturn(Optional.empty());
        when(customerRepository.findByNoHp("ghost")).thenReturn(Optional.empty());

        assertThat(service.findCustomer("ghost")).isEmpty();
    }
}
