package com.binar.bc.saku_ku.service;

import com.binar.bc.saku_ku.entity.AppCustomerEntity;
import com.binar.bc.saku_ku.entity.CustomerEntity;
import com.binar.bc.saku_ku.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AppCustomerDetailsService {

    private final CustomerRepository customerRepository;

    @Transactional(readOnly = true)
    public Optional<AppCustomerEntity> findCustomer(String identifier) {
        Optional<CustomerEntity> found = customerRepository.findByEmail(identifier);
        if (found.isEmpty()) {
            found = customerRepository.findByNoHp(identifier);
        }
        return found.map(this::toAppCustomer);
    }

    private AppCustomerEntity toAppCustomer(CustomerEntity customer) {
        AppCustomerEntity appCustomer = new AppCustomerEntity();
        appCustomer.setId(customer.getId());
        appCustomer.setUsername(customer.getEmail()); // dipakai sebagai "subject" di JWT
        appCustomer.setPassword(customer.getPasswordHash());
        return appCustomer;
    }
}