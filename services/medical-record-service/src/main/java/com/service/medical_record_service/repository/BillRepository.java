package com.service.medical_record_service.repository;

import com.service.medical_record_service.entity.Bill;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface BillRepository extends JpaRepository<Bill, UUID> {

  List<Bill> findByPatientId(UUID patientId);

  List<Bill> findByBranchId(UUID branchId);

}