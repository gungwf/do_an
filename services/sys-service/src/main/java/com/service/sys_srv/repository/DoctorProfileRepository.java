package com.service.sys_srv.repository;

import com.service.sys_srv.entity.DoctorProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface DoctorProfileRepository extends JpaRepository<DoctorProfile, UUID> {
}