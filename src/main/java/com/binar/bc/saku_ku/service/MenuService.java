package com.binar.bc.saku_ku.service;

import com.binar.bc.saku_ku.dto.MenuRequest;
import com.binar.bc.saku_ku.entity.MenuEntity;
import com.binar.bc.saku_ku.repository.MenuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MenuService {

    private final MenuRepository menuRepository;

    public List<MenuEntity> getAllMenus() {
        return menuRepository.findAll();
    }

    @Transactional
    public MenuEntity createMenu(MenuRequest request) {
        MenuEntity menu = new MenuEntity();
        menu.setNamaMenu(request.getNamaMenu());
        menu.setPath(request.getPath());
        menu.setIcon(request.getIcon());
        menu.setParentId(request.getParentId());
        menu.setUrutan(request.getUrutan() != null ? request.getUrutan() : 0);
        menu.setStatus("ACTIVE");
        return menuRepository.save(menu);
    }

    @Transactional
    public MenuEntity updateMenu(UUID id, MenuRequest request) {
        MenuEntity menu = menuRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Menu not found with id: " + id));

        if (request.getNamaMenu() != null) menu.setNamaMenu(request.getNamaMenu());
        if (request.getPath() != null) menu.setPath(request.getPath());
        if (request.getIcon() != null) menu.setIcon(request.getIcon());
        if (request.getParentId() != null) menu.setParentId(request.getParentId());
        if (request.getUrutan() != null) menu.setUrutan(request.getUrutan());
        if (request.getStatus() != null) menu.setStatus(request.getStatus());

        return menuRepository.save(menu);
    }

    @Transactional
    public void deleteMenu(UUID id) {
        if (!menuRepository.existsById(id)) {
            throw new RuntimeException("Menu not found with id: " + id);
        }
        menuRepository.deleteById(id);
    }
}
