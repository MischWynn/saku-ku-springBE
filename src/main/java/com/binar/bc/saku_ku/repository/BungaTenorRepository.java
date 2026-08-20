package com.binar.bc.saku_ku.repository;

import com.binar.bc.saku_ku.entity.BungaTenorEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BungaTenorRepository extends JpaRepository<BungaTenorEntity, UUID> {

    //Ini buat ngecek untuk 1 tenor spesifik ketika customer nyari buat suku bunganya berapa
    Optional<BungaTenorEntity> findByTenor(Integer tenor);

    //Ini buat ngecek untuk staff ketika mau ngecek list tenor yang aktif by status di database
    List<BungaTenorEntity> findByStatus(String status);
    
    //Ini buat validasi pas create biar ga nyoba masukkin tenor yang udah ada duplikatnya
    boolean existsByTenor(Integer tenor);
}
