package com.service.medical_record_service.entity;

import com.service.medical_record_service.entity.Enum.BillStatus;
import com.service.medical_record_service.entity.Enum.BillType;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "bills")
@Data
public class Bill {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "patient_id")
  private UUID patientId;

  @Column(name = "creator_id")
  private UUID creatorId;

  @Column(name = "branch_id", nullable = false)
  private UUID branchId;

  @Column(name = "total_amount", nullable = false)
  private BigDecimal totalAmount;

  @Column(name = "currency", nullable = false)
  private String currency = "VND";

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private BillStatus status = BillStatus.PENDING;

  @Enumerated(EnumType.STRING)
  @Column(name = "bill_type", nullable = false)
  private BillType billType;

  @Column(name = "paid_at")
  private Instant paidAt;

  @Column(name = "raw_response", columnDefinition = "TEXT")
  private String rawResponse;

  @CreationTimestamp
  @Column(name = "created_at")
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private Instant updatedAt;
}