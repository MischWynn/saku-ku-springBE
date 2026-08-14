package com.binar.bc.saku_ku.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.UUID;
import java.util.Optional;
import java.util.List;

import com.binar.bc.saku_ku.entity.UserEntity;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByEmail(String email);

    Optional<UserEntity> findByUsername(String username);

    Optional<UserEntity> findByEmailAndDeletedDateIsNull(String email);
    
    @Query("Select u from UserEntity u where u.status = 'ACTIVE'")
    List<UserEntity> findUserWhereStatusIsActive();
    
    Optional<UserEntity> findByUsernameAndDeletedDateIsNull(String username);
}
