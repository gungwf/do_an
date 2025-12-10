package com.service.medical_record_service.service;

import com.service.medical_record_service.client.client.AppointmentServiceClient;
import com.service.medical_record_service.client.dto.PagedAppointmentResponse;
import com.service.medical_record_service.client.dto.AppointmentResponseDto;
import com.service.medical_record_service.client.client.ProductInventoryClient;
import com.service.medical_record_service.client.dto.DeductStockRequest;
import com.service.medical_record_service.client.dto.InternalStatusUpdateRequest;
import com.service.medical_record_service.dto.request.MedicalRecordRequest;
import com.service.medical_record_service.dto.request.PrescriptionItemRequest;
import com.service.medical_record_service.dto.request.UpdateMedicalRecordRequest;
import com.service.medical_record_service.dto.response.MedicalRecordDetailResponse;
import com.service.medical_record_service.dto.response.PerformedServiceDto;
import com.service.medical_record_service.dto.response.PrescriptionItemDto;
import com.service.medical_record_service.dto.response.StockShortage;
import com.service.medical_record_service.entity.*;
import com.service.medical_record_service.entity.Enum.BillType;
import com.service.medical_record_service.exception.AppException;
import com.service.medical_record_service.exception.ERROR_CODE;
import com.service.medical_record_service.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MedicalRecordService {
    private final MedicalRecordRepository medicalRecordRepository;
    private final ProductInventoryClient productInventoryClient;
    private final AppointmentServiceClient appointmentServiceClient;
    private final DiagnosisTemplateRepository templateRepository;
    private final ServiceRepository serviceRepository;
    private final ServiceMaterialRepository serviceMaterialRepository;
    private final PrescriptionItemRepository prescriptionItemRepository;
    private final MedicalRecordServiceLinkRepository medicalRecordServiceLinkRepository;


    @Transactional
    public MedicalRecord createMedicalRecord(MedicalRecordRequest request) {
        // 1. Kiểm tra và khởi tạo (Giữ nguyên)
        if (medicalRecordRepository.findByAppointmentId(request.getAppointmentId()).isPresent()) {
            throw new IllegalStateException("Medical record for this appointment already exists.");
        }
        MedicalRecord medicalRecord = new MedicalRecord();
        medicalRecord.setAppointmentId(request.getAppointmentId());
        medicalRecord.setDiagnosis(request.getDiagnosis());
        medicalRecord.setIcd10Code(request.getIcd10Code());

        // 2. Lưu danh sách Dịch vụ (Giữ nguyên logic mapping)
        if (request.getServiceIds() != null) {
            List<MedicalRecordServiceLink> serviceLinks = request.getServiceIds().stream().map(serviceId -> {
                MedicalRecordServiceId id = new MedicalRecordServiceId();
                id.setMedicalRecordId(medicalRecord.getId());
                id.setServiceId(serviceId);
                MedicalRecordServiceLink link = new MedicalRecordServiceLink();
                link.setId(id);
                link.setMedicalRecord(medicalRecord);
                return link;
            }).toList();
            medicalRecord.setPerformedServices(serviceLinks);
        }

        // 3. Lưu danh sách Thuốc (Giữ nguyên logic mapping)
        if (request.getPrescriptionItems() != null) {
            List<PrescriptionItem> prescriptionEntities = request.getPrescriptionItems().stream().map(itemReq -> {
                PrescriptionItem item = new PrescriptionItem();
                item.setProductId(itemReq.productId());
                item.setQuantity(itemReq.quantity());
                item.setDosage(itemReq.dosage());
                item.setMedicalRecord(medicalRecord);
                return item;
            }).toList();
            medicalRecord.setPrescriptionItems(prescriptionEntities);
        }

        // 4. Lưu bệnh án
        MedicalRecord saved = medicalRecordRepository.save(medicalRecord);

        // 5. Cập nhật trạng thái lịch hẹn -> CREATED_MEDICAL_RECORD
        try {
            appointmentServiceClient.updateAppointmentStatusInternal(
                saved.getAppointmentId(),
                new InternalStatusUpdateRequest("PENDING_BILLING")
            );
        } catch (Exception e) {
            log.warn("Không thể cập nhật trạng thái lịch hẹn sang PENDING_BILLING: {}", e.getMessage());
        }

        return saved;
    }

        /**
         * Lấy danh sách medical records của một bệnh nhân (dựa vào appointmentId)
         * Sử dụng token để lấy patientId ở controller, service nhận patientId để lấy appointments
         */
        public Page<com.service.medical_record_service.dto.response.MedicalRecordWithAppointmentResponse> getMedicalRecordsForPatient(java.util.UUID patientId, int page, int size, String sortBy) {
            // 1. Gọi appointment-service để lấy danh sách appointment (paged) của bệnh nhân
            PagedAppointmentResponse apptPage = null;
            try {
                apptPage = appointmentServiceClient.getAppointmentsForPatient(patientId, page, size);
            } catch (Exception e) {
                log.warn("Không lấy được appointment của patient {}: {}", patientId, e.getMessage());
            }

            List<java.util.UUID> apptIds = java.util.Collections.emptyList();
            if (apptPage != null && apptPage.content() != null) {
                apptIds = apptPage.content().stream().map(AppointmentResponseDto::id).toList();
            }

            if (apptIds.isEmpty()) {
                return Page.empty();
            }

            Pageable pageable;
            if (sortBy == null || sortBy.isBlank()) {
                pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
            } else {
                pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, sortBy));
            }

            Page<MedicalRecord> recordsPage = medicalRecordRepository.findByAppointmentIdIn(apptIds, pageable);

            var content = recordsPage.getContent().stream().map(r -> {
                com.service.medical_record_service.client.dto.AppointmentResponseDto appt = null;
                try {
                    appt = appointmentServiceClient.getAppointmentById(r.getAppointmentId());
                } catch (Exception ex) {
                    log.warn("Không lấy được appointment {} cho medicalRecord {}: {}", r.getAppointmentId(), r.getId(), ex.getMessage());
                }
                return new com.service.medical_record_service.dto.response.MedicalRecordWithAppointmentResponse(
                        r.getId(), appt, r.getDiagnosis(), r.getCreatedAt(), r.getUpdatedAt()
                );
            }).toList();

            return new PageImpl<>(content, pageable, recordsPage.getTotalElements());
        }

    public MedicalRecord getRecordByAppointmentId(UUID appointmentId) {
        return medicalRecordRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new AppException(ERROR_CODE.PRODUCT_NOT_FOUND));
    }

    public MedicalRecordDetailResponse getRecordDetailByAppointmentId(UUID appointmentId) {
        MedicalRecord record = getRecordByAppointmentId(appointmentId);

            List<PerformedServiceDto> performed = new ArrayList<>();
            if (record.getPerformedServices() != null) {
                for (MedicalRecordServiceLink link : record.getPerformedServices()) {
                    var svcId = link.getId().getServiceId();
                    var svcOpt = serviceRepository.findById(svcId);
                    svcOpt.ifPresent(svc -> {
                        link.setServiceName(svc.getServiceName());
                        link.setPrice(svc.getPrice());
                        performed.add(new PerformedServiceDto(svc.getId(), svc.getServiceName(), svc.getPrice()));
                    });
                }
            }

            List<PrescriptionItemDto> prescriptionItems = new ArrayList<>();
            if (record.getPrescriptionItems() != null) {
                for (PrescriptionItem pi : record.getPrescriptionItems()) {
                    String productName = null;
                    try {
                        var prod = productInventoryClient.getProductById(pi.getProductId());
                        if (prod != null) productName = prod.productName();
                    } catch (Exception e) {
                        log.warn("Không lấy được tên sản phẩm {}: {}", pi.getProductId(), e.getMessage());
                    }
                    prescriptionItems.add(new PrescriptionItemDto(
                        pi.getId(),
                        pi.getProductId(),
                        pi.getQuantity(),
                        pi.getDosage(),
                        pi.getNotes(),
                        productName
                    ));
                }
            }

            return new MedicalRecordDetailResponse(
                record.getId(),
                record.getAppointmentId(),
                record.getDiagnosis(),
                record.getIcd10Code(),
                record.isLocked(),
                record.getESignature(),
                record.getCreatedAt(),
                record.getUpdatedAt(),
                performed,
                prescriptionItems
            );
    }

        /**
         * Lấy chi tiết bệnh án theo id bệnh án
         * Trả về chi tiết gồm danh sách dịch vụ đã thực hiện và đơn thuốc
         */
        public MedicalRecordDetailResponse getRecordDetailById(UUID medicalRecordId) {
            MedicalRecord record = medicalRecordRepository.findById(medicalRecordId)
                    .orElseThrow(() -> new AppException(ERROR_CODE.MEDICAL_RECORD_NOT_FOUND));

            List<PerformedServiceDto> performed = new ArrayList<>();
            if (record.getPerformedServices() != null) {
                for (MedicalRecordServiceLink link : record.getPerformedServices()) {
                    var svcId = link.getId().getServiceId();
                    var svcOpt = serviceRepository.findById(svcId);
                    svcOpt.ifPresent(svc -> {
                        link.setServiceName(svc.getServiceName());
                        link.setPrice(svc.getPrice());
                        performed.add(new PerformedServiceDto(svc.getId(), svc.getServiceName(), svc.getPrice()));
                    });
                }
            }

            List<PrescriptionItemDto> prescriptionItems = new ArrayList<>();
            if (record.getPrescriptionItems() != null) {
                for (PrescriptionItem pi : record.getPrescriptionItems()) {
                    String productName = null;
                    try {
                        var prod = productInventoryClient.getProductById(pi.getProductId());
                        if (prod != null) productName = prod.productName();
                    } catch (Exception e) {
                        log.warn("Không lấy được tên sản phẩm {}: {}", pi.getProductId(), e.getMessage());
                    }
                    prescriptionItems.add(new PrescriptionItemDto(
                        pi.getId(),
                        pi.getProductId(),
                        pi.getQuantity(),
                        pi.getDosage(),
                        pi.getNotes(),
                        productName
                    ));
                }
            }

            return new MedicalRecordDetailResponse(
                record.getId(),
                record.getAppointmentId(),
                record.getDiagnosis(),
                record.getIcd10Code(),
                record.isLocked(),
                record.getESignature(),
                record.getCreatedAt(),
                record.getUpdatedAt(),
                performed,
                prescriptionItems
            );
        }

    public MedicalRecord lockMedicalRecord(UUID recordId, String signatureData) {
        MedicalRecord medicalRecord = medicalRecordRepository.findById(recordId)
                .orElseThrow(() -> new AppException(ERROR_CODE.MEDICAL_RECORD_NOT_FOUND));

        if (medicalRecord.isLocked()) {
            throw new AppException(ERROR_CODE.MEDICAL_RECORD_LOCKED);
        }

        medicalRecord.setESignature(signatureData);
        medicalRecord.setLocked(true);

        return medicalRecordRepository.save(medicalRecord);
    }

    public BigDecimal calculateBillTotal(UUID medicalRecordId, BillType billType) {
        MedicalRecord record = medicalRecordRepository.findById(medicalRecordId)
            .orElseThrow(() -> new RuntimeException("Medical Record not found"));

        BigDecimal total = BigDecimal.ZERO;

        // TRƯỜNG HỢP 1: TÍNH TIỀN DỊCH VỤ
        if (billType == BillType.SERVICE_PAYMENT) {
            if (record.getPerformedServices() != null) {
                for (MedicalRecordServiceLink link : record.getPerformedServices()) {
                    // Giả sử Service có trường price
                    // (Cần inject ServiceRepository nếu chưa có)
                    var service = serviceRepository.findById(link.getId().getServiceId())
                        .orElseThrow(() -> new RuntimeException("Service not found"));

                    total = total.add(service.getPrice());
                }
            }
        }

        // TRƯỜNG HỢP 2: TÍNH TIỀN THUỐC
        else if (billType == BillType.DRUG_PAYMENT) {
            if (record.getPrescriptionItems() != null) {
                for (PrescriptionItem item : record.getPrescriptionItems()) {
                    // Gọi sang Inventory Service để lấy giá thuốc hiện tại
                    var product = productInventoryClient.getProductById(item.getProductId());

                    BigDecimal itemTotal = product.price().multiply(BigDecimal.valueOf(item.getQuantity()));
                    total = total.add(itemTotal);
                }
            }
        }

        return total;
    }

    @Transactional
    public void triggerDeductStock(UUID medicalRecordId) {
        MedicalRecord medicalRecord = medicalRecordRepository.findById(medicalRecordId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Bệnh án: " + medicalRecordId));

        UUID branchId;
        try {
            // Gọi sang appointment-service để lấy thông tin lịch hẹn
            var appointmentDto = appointmentServiceClient.getAppointmentById(medicalRecord.getAppointmentId());
            if (appointmentDto == null || appointmentDto.branch() == null || appointmentDto.branch().id() == null) {
                throw new RuntimeException("Không thể lấy thông tin chi nhánh từ lịch hẹn.");
            }
            branchId = appointmentDto.branch().id();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi giao tiếp dịch vụ: " + e.getMessage());
        }

        // 3. Trừ kho Đơn thuốc (Prescription Items)
        if (medicalRecord.getPrescriptionItems() != null && !medicalRecord.getPrescriptionItems().isEmpty()) {
            for (PrescriptionItem item : medicalRecord.getPrescriptionItems()) {
                DeductStockRequest deductRequest = new DeductStockRequest();
                deductRequest.setBranchId(branchId);
                deductRequest.setProductId(item.getProductId());
                deductRequest.setQuantityToDeduct(item.getQuantity());

                // Gọi sang product-inventory-service
                // Nếu hết hàng, service kia sẽ ném Exception và toàn bộ hàm này sẽ rollback
                productInventoryClient.deductStock(deductRequest);
            }
        }

        // 4. Trừ kho Vật tư tiêu hao (BOM) cho các Dịch vụ đã thực hiện
        if (medicalRecord.getPerformedServices() != null && !medicalRecord.getPerformedServices().isEmpty()) {
            for (MedicalRecordServiceLink serviceLink : medicalRecord.getPerformedServices()) {
                UUID serviceId = serviceLink.getId().getServiceId();

                // Lấy danh sách BOM cho dịch vụ này
                List<ServiceMaterial> materials = serviceMaterialRepository.findById_ServiceId(serviceId);

                for (ServiceMaterial material : materials) {
                    DeductStockRequest deductRequest = new DeductStockRequest();
                    deductRequest.setBranchId(branchId);
                    deductRequest.setProductId(material.getId().getProductId());
                    deductRequest.setQuantityToDeduct(material.getQuantityConsumed());

                    productInventoryClient.deductStock(deductRequest);
                }
            }
        }
    }

    /**
     * Kiểm tra tồn kho cho medicalRecord (không trừ). Trả về danh sách shortage nếu có.
     */
    public List<StockShortage> checkStock(UUID medicalRecordId) {
        MedicalRecord medicalRecord = medicalRecordRepository.findById(medicalRecordId)
                .orElseThrow(() -> new RuntimeException("Medical Record not found"));

        UUID branchId;
        try {
            var appointmentDto = appointmentServiceClient.getAppointmentById(medicalRecord.getAppointmentId());
            if (appointmentDto == null || appointmentDto.branch() == null || appointmentDto.branch().id() == null) {
                throw new RuntimeException("Cannot resolve branchId from appointment");
            }
            branchId = appointmentDto.branch().id();
        } catch (Exception e) {
            throw new RuntimeException("Error fetching appointment: " + e.getMessage());
        }

        List<StockShortage> shortages = new ArrayList<>();

        // Check prescription items
        if (medicalRecord.getPrescriptionItems() != null) {
            for (PrescriptionItem item : medicalRecord.getPrescriptionItems()) {
                try {
                    // Log before calling inventory to help debug branch/product mismatch
                    log.info("checkStock: medicalRecordId={}, checking product {} at branch {}", medicalRecordId, item.getProductId(), branchId);
                    var apiResp = productInventoryClient.getInventoryByBranchAndProduct(branchId, item.getProductId());
                    var inv = apiResp == null ? null : apiResp.result();
                    int available = (inv == null || inv.quantity() == null) ? 0 : inv.quantity();
                    log.info("checkStock: product={}, branch={}, available={}, required={}", item.getProductId(), branchId, available, item.getQuantity());
                    if (available < item.getQuantity()) {
                        shortages.add(new StockShortage(item.getProductId(), item.getQuantity(), available));
                    }
                } catch (Exception e) {
                    // treat as not available
                    log.warn("checkStock: error fetching inventory for product {} at branch {}: {}", item.getProductId(), branchId, e.getMessage());
                    shortages.add(new StockShortage(item.getProductId(), item.getQuantity(), 0));
                }
            }
        }

        // Check service materials (BOM)
        if (medicalRecord.getPerformedServices() != null) {
            for (MedicalRecordServiceLink sl : medicalRecord.getPerformedServices()) {
                UUID serviceId = sl.getId().getServiceId();
                List<ServiceMaterial> materials = serviceMaterialRepository.findById_ServiceId(serviceId);
                for (ServiceMaterial m : materials) {
                    try {
                            // Log service material checks
                            log.info("checkStock: medicalRecordId={}, checking service material product {} at branch {}", medicalRecordId, m.getId().getProductId(), branchId);
                            var apiResp2 = productInventoryClient.getInventoryByBranchAndProduct(branchId, m.getId().getProductId());
                            var inv = apiResp2 == null ? null : apiResp2.result();
                            int available = (inv == null || inv.quantity() == null) ? 0 : inv.quantity();
                            int required = m.getQuantityConsumed();
                            log.info("checkStock: product={}, branch={}, available={}, required={}", m.getId().getProductId(), branchId, available, required);
                            if (available < required) {
                                shortages.add(new StockShortage(m.getId().getProductId(), required, available));
                            }
                    } catch (Exception e) {
                            log.warn("checkStock: error fetching inventory for product {} at branch {}: {}", m.getId().getProductId(), branchId, e.getMessage());
                        shortages.add(new StockShortage(m.getId().getProductId(), m.getQuantityConsumed(), 0));
                    }
                }
            }
        }

        return shortages;
    }

    @Transactional
    public MedicalRecord updateMedicalRecord(UUID medicalRecordId, UpdateMedicalRecordRequest request) {

        // 1. Tìm và kiểm tra trạng thái bệnh án
        MedicalRecord medicalRecord = medicalRecordRepository.findById(medicalRecordId)
                .orElseThrow(() -> new AppException(ERROR_CODE.MEDICAL_RECORD_NOT_FOUND));

        if (medicalRecord.isLocked()) {
            throw new IllegalStateException("Không thể cập nhật bệnh án đã bị khóa.");
        }

        // 2. Cập nhật thông tin chẩn đoán
        medicalRecord.setDiagnosis(request.getDiagnosis());
        medicalRecord.setIcd10Code(request.getIcd10Code());

        // 3. Giữ nguyên performedServices (không cập nhật dịch vụ trong lần update này)

        // 4a. Thêm mới đơn thuốc (append) nếu có trong request
        if (request.getPrescriptionItems() != null && !request.getPrescriptionItems().isEmpty()) {
            if (medicalRecord.getPrescriptionItems() == null) {
                medicalRecord.setPrescriptionItems(new ArrayList<>());
            }
            for (PrescriptionItemRequest itemRequest : request.getPrescriptionItems()) {
                PrescriptionItem item = new PrescriptionItem();
                item.setProductId(itemRequest.productId());
                item.setQuantity(itemRequest.quantity());
                item.setDosage(itemRequest.dosage());
                item.setMedicalRecord(medicalRecord);
                medicalRecord.getPrescriptionItems().add(item);
            }
        }

        // 4b. Nếu có templateId, load các đơn thuốc từ template và append vào bệnh án
        if (request.getTemplateId() != null) {
            var template = templateRepository.findById(request.getTemplateId())
                    .orElseThrow(() -> new AppException(ERROR_CODE.MEDICAL_RECORD_NOT_FOUND));
            if (template.getPrescriptionItems() != null && !template.getPrescriptionItems().isEmpty()) {
                if (medicalRecord.getPrescriptionItems() == null) {
                    medicalRecord.setPrescriptionItems(new ArrayList<>());
                }
                template.getPrescriptionItems().forEach(ti -> {
                    PrescriptionItem pi = new PrescriptionItem();
                    pi.setProductId(ti.getProductId());
                    pi.setQuantity(ti.getQuantity());
                    pi.setDosage(ti.getDosage());
                    pi.setMedicalRecord(medicalRecord);
                    medicalRecord.getPrescriptionItems().add(pi);
                });
            }
        }

        // 5. Lưu bệnh án với các đơn thuốc mới thêm
        MedicalRecord saved = medicalRecordRepository.save(medicalRecord);

        // 6. Cập nhật trạng thái lịch hẹn thành hoàn thành (COMPLETED)
        try {
            appointmentServiceClient.updateAppointmentStatusInternal(
                saved.getAppointmentId(),
                new InternalStatusUpdateRequest("COMPLETED")
            );
        } catch (Exception e) {
            log.warn("Không thể cập nhật trạng thái lịch hẹn sang COMPLETED: {}", e.getMessage());
        }

        return saved;
    }
}