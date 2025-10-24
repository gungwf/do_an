package com.service.medical_record_service.service;

import com.service.medical_record_service.client.client.AppointmentServiceClient;
import com.service.medical_record_service.client.client.ProductInventoryClient;
import com.service.medical_record_service.client.dto.AppointmentResponseDto;
import com.service.medical_record_service.client.dto.DeductStockRequest;
import com.service.medical_record_service.client.dto.ProductDto;
import com.service.medical_record_service.dto.request.MedicalRecordRequest;
import com.service.medical_record_service.dto.request.PrescriptionItemRequest;
import com.service.medical_record_service.dto.request.UpdateMedicalRecordRequest;
import com.service.medical_record_service.entity.*;
import com.service.medical_record_service.exception.AppException;
import com.service.medical_record_service.exception.ERROR_CODE;
import com.service.medical_record_service.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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
    // 1. Kiểm tra xem lịch hẹn này đã có bệnh án chưa
//    if (medicalRecordRepository.findByAppointmentId(request.getAppointmentId()).isPresent()) {
//        throw new IllegalStateException("Medical record for this appointment already exists.");
//    }
//
//    MedicalRecord medicalRecord = new MedicalRecord();
//    medicalRecord.setAppointmentId(request.getAppointmentId());
//
//    // 2. Lấy thông tin chi nhánh từ appointment-service (cần cho việc trừ kho)
//    AppointmentResponseDto appointment = appointmentServiceClient.getAppointmentById(request.getAppointmentId());
//    if (appointment.branch() == null || appointment.branch().id() == null) {
//        // Tạm thời dùng IllegalStateException, bạn có thể đổi thành AppException
//        throw new IllegalStateException("Không thể xác định chi nhánh từ lịch hẹn ID: " + request.getAppointmentId());
//    }
//    UUID branchId = appointment.branch().id();
//
//
//    List<PrescriptionItemRequest> itemsToProcess = new ArrayList<>();
//
//    // Áp dụng template hoặc dùng dữ liệu thủ công
//    if (request.getTemplateId() != null) {
//        // --- TRƯỜNG HỢP DÙNG TEMPLATE ---
//        DiagnosisTemplate template = templateRepository.findById(request.getTemplateId())
//                .orElseThrow(() -> new RuntimeException("Template not found with id: " + request.getTemplateId()));
//
//        // Sao chép thông tin từ template vào bệnh án
//        medicalRecord.setDiagnosis(template.getDiagnosisContent());
//        medicalRecord.setIcd10Code(template.getIcd10Code());
//
//        // Sao chép các mục trong đơn thuốc của template để xử lý tiếp
//        if (template.getPrescriptionItems() != null) {
//            itemsToProcess = template.getPrescriptionItems().stream()
//                    .map(item -> new PrescriptionItemRequest(
//                            item.getProductId(),
//                            item.getQuantity(),
//                            item.getDosage()))
//                    .collect(Collectors.toList());
//        }
//    } else {
//        // --- TRƯỜNG HỢP TẠO THỦ CÔNG ---
//        medicalRecord.setDiagnosis(request.getDiagnosis());
//        medicalRecord.setIcd10Code(request.getIcd10Code());
//        if (request.getPrescriptionItems() != null) {
//            itemsToProcess = request.getPrescriptionItems();
//        }
//    }
//
//    // 4. Xử lý đơn thuốc (trừ kho và tạo entity)
//    if (!itemsToProcess.isEmpty()) {
//        List<PrescriptionItem> prescriptionEntities = new ArrayList<>();
//        for (PrescriptionItemRequest itemRequest : itemsToProcess) {
//            // Gọi sang product-inventory-service để trừ kho
//            DeductStockRequest deductRequest = new DeductStockRequest();
//            deductRequest.setBranchId(branchId);
//            deductRequest.setProductId(itemRequest.productId());
//            deductRequest.setQuantityToDeduct(itemRequest.quantity());
//            productInventoryClient.deductStock(deductRequest);
//
//            // Tạo entity PrescriptionItem
//            PrescriptionItem item = new PrescriptionItem();
//            item.setProductId(itemRequest.productId());
//            item.setQuantity(itemRequest.quantity());
//            item.setDosage(itemRequest.dosage());
//            item.setMedicalRecord(medicalRecord); // Liên kết ngược lại
//            prescriptionEntities.add(item);
//        }
//        medicalRecord.setPrescriptionItems(prescriptionEntities);
//    }
//
//    if (request.getServiceIds() != null && !request.getServiceIds().isEmpty()) {
//        List<MedicalRecordServiceLink> serviceLinks = new ArrayList<>();
//        for (UUID serviceId : request.getServiceIds()) {
//            // (Bạn có thể thêm logic gọi serviceRepository.findById(serviceId)
//            // để kiểm tra xem serviceId có thật không)
//
//            MedicalRecordServiceId linkId = new MedicalRecordServiceId();
//            linkId.setMedicalRecordId(medicalRecord.getId()); // Sẽ được gán sau khi save
//            linkId.setServiceId(serviceId);
//
//            MedicalRecordServiceLink link = new MedicalRecordServiceLink();
//            link.setId(linkId);
//            link.setMedicalRecord(medicalRecord); // Liên kết ngược lại
//
//            serviceLinks.add(link);
//        }
//        medicalRecord.setPerformedServices(serviceLinks);
//    }
//
//    return medicalRecordRepository.save(medicalRecord);

    if (medicalRecordRepository.findByAppointmentId(request.getAppointmentId()).isPresent()) {
        throw new IllegalStateException("Medical record for this appointment already exists.");
    }

    MedicalRecord medicalRecord = new MedicalRecord();
    medicalRecord.setAppointmentId(request.getAppointmentId());
    medicalRecord.setDiagnosis(request.getDiagnosis());
    medicalRecord.setIcd10Code(request.getIcd10Code());

    // 2. Xử lý danh sách DỊCH VỤ đã thực hiện
    if (request.getServiceIds() != null && !request.getServiceIds().isEmpty()) {
        List<MedicalRecordServiceLink> serviceLinks = new ArrayList<>();
        for (UUID serviceId : request.getServiceIds()) {
            MedicalRecordServiceId linkId = new MedicalRecordServiceId();
            linkId.setMedicalRecordId(medicalRecord.getId()); // Sẽ được gán khi save
            linkId.setServiceId(serviceId);

            MedicalRecordServiceLink link = new MedicalRecordServiceLink();
            link.setId(linkId);
            link.setMedicalRecord(medicalRecord);
            serviceLinks.add(link);
        }
        medicalRecord.setPerformedServices(serviceLinks);
    }

    // 3. Xử lý danh sách ĐƠN THUỐC
    if (request.getPrescriptionItems() != null && !request.getPrescriptionItems().isEmpty()) {
        List<PrescriptionItem> prescriptionEntities = new ArrayList<>();
        for (PrescriptionItemRequest itemRequest : request.getPrescriptionItems()) {
            PrescriptionItem item = new PrescriptionItem();
            item.setProductId(itemRequest.productId());
            item.setQuantity(itemRequest.quantity());
            item.setDosage(itemRequest.dosage());
            item.setMedicalRecord(medicalRecord);
            prescriptionEntities.add(item);
        }
        medicalRecord.setPrescriptionItems(prescriptionEntities);
    }

    // 4. Lưu bệnh án (KHÔNG tính tiền, KHÔNG trừ kho)
    return medicalRecordRepository.save(medicalRecord);
}

    public MedicalRecord getRecordByAppointmentId(UUID appointmentId) {
        return medicalRecordRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new AppException(ERROR_CODE.PRODUCT_NOT_FOUND));
    }

    public MedicalRecord lockMedicalRecord(UUID recordId, String signatureData) {
        MedicalRecord medicalRecord = medicalRecordRepository.findById(recordId)
                .orElseThrow(() -> new AppException(ERROR_CODE.MEDICAL_RECORD_NOT_FOUND)); // Cần thêm mã lỗi này

        if (medicalRecord.isLocked()) {
            throw new AppException(ERROR_CODE.MEDICAL_RECORD_LOCKED);
        }

        medicalRecord.setESignature(signatureData); // signatureData có thể là tên bác sĩ, hoặc một chuỗi base64 của hình ảnh chữ ký
        medicalRecord.setLocked(true);

        return medicalRecordRepository.save(medicalRecord);
    }

    public BigDecimal calculateBillTotal(UUID medicalRecordId) {
        MedicalRecord medicalRecord = medicalRecordRepository.findById(medicalRecordId)
                .orElseThrow(() -> new RuntimeException("Medical record not found"));

        // 1. Tính tổng tiền dịch vụ
        BigDecimal servicesTotal = BigDecimal.ZERO;
        if (medicalRecord.getPerformedServices() != null) {
            for (MedicalRecordServiceLink link : medicalRecord.getPerformedServices()) {
                com.service.medical_record_service.entity.Service service
                        = serviceRepository.findById(link.getId().getServiceId())
                        .orElseThrow(() -> new RuntimeException("Service not found"));
                servicesTotal = servicesTotal.add(service.getPrice());
            }
        }

        // 2. Tính tổng tiền thuốc
        BigDecimal productsTotal = BigDecimal.ZERO;
        if (medicalRecord.getPrescriptionItems() != null) {
            for (PrescriptionItem item : medicalRecord.getPrescriptionItems()) {
                ProductDto product = productInventoryClient.getProductById(item.getProductId());
                productsTotal = productsTotal.add(product.price().multiply(new BigDecimal(item.getQuantity())));
            }
        }

        return servicesTotal.add(productsTotal);
    }

    @Transactional
    public void triggerDeductStock(UUID medicalRecordId) {
        log.info("Bắt đầu trigger trừ kho cho Bệnh án ID: {}", medicalRecordId);

        // 1. Lấy Bệnh án và các liên kết của nó
        MedicalRecord medicalRecord = medicalRecordRepository.findById(medicalRecordId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Bệnh án: " + medicalRecordId));

        // 2. Lấy Chi nhánh (Branch ID) từ Lịch hẹn
        // Việc này là bắt buộc để biết trừ kho ở đâu
        UUID branchId;
        try {
            // Gọi sang appointment-service để lấy thông tin lịch hẹn
            var appointmentDto = appointmentServiceClient.getAppointmentById(medicalRecord.getAppointmentId());
            if (appointmentDto == null || appointmentDto.branch() == null || appointmentDto.branch().id() == null) {
                throw new RuntimeException("Không thể lấy thông tin chi nhánh từ lịch hẹn.");
            }
            branchId = appointmentDto.branch().id();
            log.info("Đã xác định Chi nhánh: {}", branchId);
        } catch (Exception e) {
            log.error("Lỗi nghiêm trọng khi gọi sang appointment-service để lấy branchId: {}", e.getMessage());
            // Ném lỗi để transaction rollback, ngăn việc trừ kho nếu không biết chi nhánh
            throw new RuntimeException("Lỗi giao tiếp dịch vụ: " + e.getMessage());
        }

        // 3. Trừ kho Đơn thuốc (Prescription Items)
        if (medicalRecord.getPrescriptionItems() != null && !medicalRecord.getPrescriptionItems().isEmpty()) {
            log.info("Đang trừ kho cho {} mục đơn thuốc...", medicalRecord.getPrescriptionItems().size());
            for (PrescriptionItem item : medicalRecord.getPrescriptionItems()) {
                DeductStockRequest deductRequest = new DeductStockRequest();
                deductRequest.setBranchId(branchId);
                deductRequest.setProductId(item.getProductId());
                deductRequest.setQuantityToDeduct(item.getQuantity());

                // Gọi sang product-inventory-service
                // Nếu hết hàng, service kia sẽ ném Exception và toàn bộ hàm này sẽ rollback
                productInventoryClient.deductStock(deductRequest);
                log.info("Đã trừ {} sản phẩm (thuốc) ID: {}", item.getQuantity(), item.getProductId());
            }
        } else {
            log.info("Bệnh án {} không có đơn thuốc để trừ kho.", medicalRecordId);
        }

        // 4. Trừ kho Vật tư tiêu hao (BOM) cho các Dịch vụ đã thực hiện
        if (medicalRecord.getPerformedServices() != null && !medicalRecord.getPerformedServices().isEmpty()) {
            log.info("Đang trừ kho vật tư tiêu hao cho {} dịch vụ...", medicalRecord.getPerformedServices().size());
            for (MedicalRecordServiceLink serviceLink : medicalRecord.getPerformedServices()) {
                UUID serviceId = serviceLink.getId().getServiceId();

                // Lấy danh sách BOM cho dịch vụ này
                List<ServiceMaterial> materials = serviceMaterialRepository.findById_ServiceId(serviceId);

                for (ServiceMaterial material : materials) {
                    DeductStockRequest deductRequest = new DeductStockRequest();
                    deductRequest.setBranchId(branchId);
                    deductRequest.setProductId(material.getId().getProductId());
                    deductRequest.setQuantityToDeduct(material.getQuantityConsumed());

                    // Gọi sang product-inventory-service
                    productInventoryClient.deductStock(deductRequest);
                    log.info("Đã trừ {} vật tư (BOM) ID: {} cho Dịch vụ ID: {}",
                            material.getQuantityConsumed(), material.getId().getProductId(), serviceId);
                }
            }
        } else {
            log.info("Bệnh án {} không có dịch vụ nào được thực hiện.", medicalRecordId);
        }

        log.info("Hoàn tất việc trừ kho cho Bệnh án ID: {}", medicalRecordId);
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

        // 3. Xóa các dịch vụ cũ và thêm lại (nếu có)
        // (Hibernate sẽ tự động xóa các bản ghi trong medical_record_services)
        medicalRecord.getPerformedServices().clear();
        if (request.getServiceIds() != null && !request.getServiceIds().isEmpty()) {
            List<MedicalRecordServiceLink> serviceLinks = new ArrayList<>();
            for (UUID serviceId : request.getServiceIds()) {
                MedicalRecordServiceId linkId = new MedicalRecordServiceId();
                linkId.setMedicalRecordId(medicalRecordId);
                linkId.setServiceId(serviceId);

                MedicalRecordServiceLink link = new MedicalRecordServiceLink();
                link.setId(linkId);
                link.setMedicalRecord(medicalRecord);
                serviceLinks.add(link);
            }
            medicalRecord.getPerformedServices().addAll(serviceLinks);
        }

        // 4. Xóa đơn thuốc cũ và thêm lại (nếu có)
        // (Hibernate sẽ tự động xóa các bản ghi trong prescription_items)
        medicalRecord.getPrescriptionItems().clear();
        if (request.getPrescriptionItems() != null && !request.getPrescriptionItems().isEmpty()) {
            List<PrescriptionItem> prescriptionEntities = new ArrayList<>();
            for (PrescriptionItemRequest itemRequest : request.getPrescriptionItems()) {
                PrescriptionItem item = new PrescriptionItem();
                item.setProductId(itemRequest.productId());
                item.setQuantity(itemRequest.quantity());
                item.setDosage(itemRequest.dosage());
                item.setMedicalRecord(medicalRecord);
                prescriptionEntities.add(item);
            }
            medicalRecord.getPrescriptionItems().addAll(prescriptionEntities);
        }

        // 5. Lưu lại Bệnh án (và các liên kết con)
        return medicalRecordRepository.save(medicalRecord);
    }
}