package com.binar.bc.saku_ku.service;

import com.binar.bc.saku_ku.entity.AppCustomerEntity;
import com.binar.bc.saku_ku.entity.CustomerEntity;
import com.binar.bc.saku_ku.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AppCustomerDetailsService {

    private final CustomerRepository customerRepository;

    @Transactional(readOnly = true)
    public Optional<AppCustomerEntity> findCustomer(String identifier) {
        String trimmed = identifier == null ? "" : identifier.trim();
        Optional<CustomerEntity> found = customerRepository.findByEmail(trimmed);
        if (found.isEmpty()) {
            found = customerRepository.findByNoHp(trimmed);
        }
        // No HP di DB formatnya campur: Android daftarin "+62812...", data lama/dummy ada yang
        // "0812...". Jadi kalau persis-nya gak ketemu, coba format lain dari nomor yang sama.
        for (String variant : phoneVariants(trimmed)) {
            if (found.isPresent()) break;
            found = customerRepository.findByNoHp(variant);
        }
        return found.map(this::toAppCustomer);
    }

    // "0812..", "62812..", "+62812..", "812.." (spasi/strip diabaikan) -> semua format lain
    // dari nomor yang sama, selain input aslinya. Bukan nomor HP (mis. email) -> kosong.
    static List<String> phoneVariants(String input) {
        String cleaned = input.replaceAll("[\\s-]", "");
        String local;
        if (cleaned.startsWith("+62")) {
            local = cleaned.substring(3);
        } else if (cleaned.startsWith("62")) {
            local = cleaned.substring(2);
        } else if (cleaned.startsWith("0")) {
            local = cleaned.substring(1);
        } else {
            local = cleaned;
        }
        if (!local.matches("8\\d{6,13}")) {
            return List.of();
        }
        Set<String> variants = new LinkedHashSet<>(List.of("+62" + local, "0" + local, "62" + local));
        variants.remove(input);
        return List.copyOf(variants);
    }

    private AppCustomerEntity toAppCustomer(CustomerEntity customer) {
        AppCustomerEntity appCustomer = new AppCustomerEntity();
        appCustomer.setId(customer.getId());
        appCustomer.setUsername(customer.getEmail()); // dipakai sebagai "subject" di JWT
        appCustomer.setPassword(customer.getPasswordHash());
        return appCustomer;
    }
}