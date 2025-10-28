package com.service.sys_srv.repository;

import com.service.sys_srv.entity.DoctorProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface DoctorProfileRepository extends JpaRepository<DoctorProfile, UUID> {
    @Query("SELECT d.specialty FROM DoctorProfile d WHERE d.specialty IS NOT NULL")
    List<String> findDistinctSpecialties();
}