package com.service.sys_srv.repository;

import com.service.sys_srv.entity.PatientProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PatientProfileRepository extends JpaRepository<PatientProfile, UUID> {
}
