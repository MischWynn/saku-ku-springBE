package com.binar.bc.saku_ku.repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;
import com.binar.bc.saku_ku.entity.RoleEntity;

import com.binar.bc.saku_ku.dto.roleDTO;

@Repository
public interface RoleRepository extends JpaRepository<RoleEntity, UUID> {
   

}
