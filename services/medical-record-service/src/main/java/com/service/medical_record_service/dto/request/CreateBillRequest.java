package com.service.medical_record_service.dto.request;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateBillRequest(UUID patientId, UUID creatorId, UUID branchId,
                                       String billType, BigDecimal totalAmount, List<BillLineRequest> items, String note) {}