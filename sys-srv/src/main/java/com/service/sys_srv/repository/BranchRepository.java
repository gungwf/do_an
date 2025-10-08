package com.service.sys_srv.repository;

import com.service.sys_srv.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BranchRepository extends JpaRepository<Branch, UUID> {
}
