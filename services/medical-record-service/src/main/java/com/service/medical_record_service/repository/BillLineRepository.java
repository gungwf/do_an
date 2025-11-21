package com.service.medical_record_service.repository;

import com.service.medical_record_service.entity.BillLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BillLineRepository extends JpaRepository<BillLine, UUID> {
    List<BillLine> findByBillId(UUID billId);
}
