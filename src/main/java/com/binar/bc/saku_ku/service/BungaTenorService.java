package com.binar.bc.saku_ku.service;

import com.binar.bc.saku_ku.dto.BungaTenorRequest;
import com.binar.bc.saku_ku.entity.BungaTenorEntity;
import com.binar.bc.saku_ku.exception.BusinessRuleException;
import com.binar.bc.saku_ku.repository.BungaTenorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BungaTenorService {

    private final BungaTenorRepository bungaTenorRepository;

    // Sengaja cuma getAll() yang di-cache, bukan getById() - getAll() yang "dibaca terus-
    // menerus" (widget simulasi cicilan, dropdown tenor pengajuan). getById() dipanggil jauh
    // lebih jarang, gak worth kompleksitas nulis cache key per-ID buat gain yang kecil.
    @Cacheable("bungaTenor")
    public List<BungaTenorEntity> getAll() {
        return bungaTenorRepository.findAll();
    }

    public BungaTenorEntity getById(UUID id) {
        return bungaTenorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bunga tenor not found with id: " + id));
    }

    @CacheEvict(value = "bungaTenor", allEntries = true)
    @Transactional
    public BungaTenorEntity create(BungaTenorRequest request) {
        if (bungaTenorRepository.existsByTenor(request.getTenor())) {
            throw new BusinessRuleException("Tenor " + request.getTenor() + " sudah terdaftar");
        }

        BungaTenorEntity entity = new BungaTenorEntity();
        entity.setTenor(request.getTenor());
        entity.setInterestRate(request.getInterestRate());
        entity.setStatus(request.getStatus() != null ? request.getStatus() : "ACTIVE");

        return bungaTenorRepository.save(entity);
    }

    @CacheEvict(value = "bungaTenor", allEntries = true)
    @Transactional
    public BungaTenorEntity update(UUID id, BungaTenorRequest request) {
        BungaTenorEntity entity = bungaTenorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bunga tenor not found with id: " + id));

        if (request.getTenor() != null) {
            entity.setTenor(request.getTenor());
        }
        if (request.getInterestRate() != null) {
            entity.setInterestRate(request.getInterestRate());
        }
        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus());
        }

        return bungaTenorRepository.save(entity);
    }

    @CacheEvict(value = "bungaTenor", allEntries = true)
    @Transactional
    public void delete(UUID id) {
        BungaTenorEntity entity = bungaTenorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bunga tenor not found with id: " + id));
        bungaTenorRepository.delete(entity);
    }
}
