package com.binar.bc.saku_ku.service;

import com.binar.bc.saku_ku.dto.PlafondRequest;
import com.binar.bc.saku_ku.entity.PlafondEntity;
import com.binar.bc.saku_ku.exception.BusinessRuleException;
import com.binar.bc.saku_ku.repository.PlafondRepository;
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
public class PlafondService {

    private final PlafondRepository plafondRepository;

    // getAll() gak punya parameter, jadi semua caller nge-share 1 entry cache yang sama
    // ("cache:plafond:SimpleKey.EMPTY" di Redis - cek pakai `redis-cli KEYS "cache:*"`).
    // Panggilan PERTAMA beneran query DB & isi cache; panggilan berikutnya (dalam 30 menit,
    // lihat TTL di RedisConfig) langsung dari Redis, DB gak disentuh sama sekali.
    @Cacheable("plafond")
    public List<PlafondEntity> getAll() {
        return plafondRepository.findAll();
    }

    // allEntries=true karena cache-nya cuma 1 entry (list lengkap) - kalau ada 1 baris baru/
    // berubah/kehapus, seluruh cache list itu jadi basi, bukan cuma 1 baris di dalamnya.
    @CacheEvict(value = "plafond", allEntries = true)
    @Transactional
    public PlafondEntity create(PlafondRequest request) {
        PlafondEntity plafond = new PlafondEntity();
        plafond.setNamaPlafond(request.getNamaPlafond());
        plafond.setDeskripsi(request.getDeskripsi());
        plafond.setTipe(request.getTipe());
        plafond.setLimitMaksimal(request.getLimitMaksimal());
        plafond.setStatus(request.getStatus() != null ? request.getStatus() : "ACTIVE");
        return plafondRepository.save(plafond);
    }

    @CacheEvict(value = "plafond", allEntries = true)
    @Transactional
    public PlafondEntity update(UUID id, PlafondRequest request) {
        PlafondEntity plafond = plafondRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("Plafond tidak ditemukan"));

        if (request.getNamaPlafond() != null) plafond.setNamaPlafond(request.getNamaPlafond());
        if (request.getDeskripsi() != null) plafond.setDeskripsi(request.getDeskripsi());
        if (request.getTipe() != null) plafond.setTipe(request.getTipe());
        if (request.getLimitMaksimal() != null) plafond.setLimitMaksimal(request.getLimitMaksimal());
        if (request.getStatus() != null) plafond.setStatus(request.getStatus());

        return plafondRepository.save(plafond);
    }

    @CacheEvict(value = "plafond", allEntries = true)
    @Transactional
    public void delete(UUID id) {
        if (!plafondRepository.existsById(id)) {
            throw new BusinessRuleException("Plafond tidak ditemukan");
        }
        plafondRepository.deleteById(id);
    }
}
