package com.service.sys_srv.repository;

import com.service.sys_srv.entity.Enum.UserRole;
import com.service.sys_srv.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {
    Optional<User> findByEmail(String email);
    List<User> findByRole(UserRole role);
}