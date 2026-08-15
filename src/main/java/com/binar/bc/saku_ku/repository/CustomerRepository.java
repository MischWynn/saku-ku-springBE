package com.binar.bc.saku_ku.repository;

import com.binar.bc.saku_ku.entity.CustomerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<CustomerEntity, UUID> {

    Optional<CustomerEntity> findByEmail(String email);

    Optional<CustomerEntity> findByNoHp(String noHp);

    Optional<CustomerEntity> findByNik(String nik);

    boolean existsByEmail(String email);

    boolean existsByNoHp(String noHp);

    boolean existsByNik(String nik);
}