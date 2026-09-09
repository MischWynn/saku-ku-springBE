package com.binar.bc.saku_ku.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.binar.bc.saku_ku.entity.RoleMenuEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoleMenuRepository extends JpaRepository<RoleMenuEntity, UUID> {

    List<RoleMenuEntity> findByRole_Id(UUID roleId);

    Optional<RoleMenuEntity> findByRole_IdAndMenu_Id(UUID roleId, UUID menuId);
}
