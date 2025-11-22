package com.service.medical_record_service.repository;

import com.service.medical_record_service.entity.Bill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;
import java.util.UUID;

public interface BillRepository extends JpaRepository<Bill, UUID>, JpaSpecificationExecutor<Bill> {

  List<Bill> findByPatientId(UUID patientId);

  List<Bill> findByBranchId(UUID branchId);

}