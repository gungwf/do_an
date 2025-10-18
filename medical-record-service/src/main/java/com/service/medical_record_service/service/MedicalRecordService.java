package com.service.medical_record_service.service;

import com.service.medical_record_service.client.client.AppointmentServiceClient;
import com.service.medical_record_service.client.client.ProductInventoryClient;
import com.service.medical_record_service.client.dto.AppointmentResponseDto;
import com.service.medical_record_service.client.dto.DeductStockRequest;
import com.service.medical_record_service.dto.request.MedicalRecordRequest;
import com.service.medical_record_service.dto.request.PrescriptionItemRequest;
import com.service.medical_record_service.entity.DiagnosisTemplate;
import com.service.medical_record_service.entity.MedicalRecord;
import com.service.medical_record_service.entity.PrescriptionItem;
import com.service.medical_record_service.exception.AppException;
import com.service.medical_record_service.exception.ERROR_CODE;
import com.service.medical_record_service.repository.DiagnosisTemplateRepository;
import com.service.medical_record_service.repository.MedicalRecordRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MedicalRecordService {
    private final MedicalRecordRepository medicalRecordRepository;
    private final ProductInventoryClient productInventoryClient;
    private final AppointmentServiceClient appointmentServiceClient;
    private final DiagnosisTemplateRepository templateRepository;

//    @Transactional
//    public MedicalRecord createMedicalRecord(MedicalRecordRequest request) {
//        if(medicalRecordRepository.findByAppointmentId(request.getAppointmentId()).isPresent()) {
//            throw new AppException(ERROR_CODE.DUPLICATE_MEDICAL_RECORD);
//        }
//
//        AppointmentResponseDto appointment = appointmentServiceClient.getAppointmentById(request.getAppointmentId());
//        if (appointment.branch() == null || appointment.branch().id() == null) {
//            throw new AppException(ERROR_CODE.BRANCH_INFO_MISSING);
//        }
//        UUID branchId = appointment.branch().id();
//
////        System.out.println("--- DEBUG: BÊN GỌI (medical-record-service) ---");
////        System.out.println("Đang xử lý cho Appointment ID: " + request.getAppointmentId());
////        System.out.println("Đã lấy được Branch ID từ Appointment: " + branchId);
//
//        if (request.getPrescriptionItems() != null && !request.getPrescriptionItems().isEmpty()) {
//            for (PrescriptionItemRequest itemRequest : request.getPrescriptionItems()) {
//                // Tạo request để gọi sang product-inventory-service
//                ProductDto product = productInventoryClient.getProductById(itemRequest.productId());
//
//                // Kiểm tra xem có phải là thuốc không
//                if (product.productType() != ProductType.MEDICINE) {
//                    throw new AppException(ERROR_CODE.INVALID_PRODUCT_TYPE_FOR_PRESCRIPTION);
//                }
//                DeductStockRequest deductRequest = new DeductStockRequest();
//                deductRequest.setBranchId(branchId);
//                deductRequest.setProductId(itemRequest.productId());
//                deductRequest.setQuantityToDeduct(itemRequest.quantity());
//
////                System.out.println("==> Sắp gọi trừ kho với Product ID: " + itemRequest.productId());
////                System.out.println("==> Sắp gọi trừ kho với Branch ID: " + branchId);
//
//                // Gọi API trừ kho. Nếu hết hàng, ném exception và dừng toàn bộ hàm này.
//                productInventoryClient.deductStock(deductRequest);
//            }
//        }
//
//        MedicalRecord medicalRecord = new MedicalRecord();
//        medicalRecord.setAppointmentId(request.getAppointmentId());
//        medicalRecord.setDiagnosis(request.getDiagnosis());
//        medicalRecord.setIcd10Code(request.getIcd10Code());
//        if (request.getPrescriptionItems() != null && !request.getPrescriptionItems().isEmpty()) {
//            List<PrescriptionItem> items = new ArrayList<>();
//            for (PrescriptionItemRequest itemRequest : request.getPrescriptionItems()) {
//                // Gọi sang product service để xác thực sản phẩm
//                productInventoryClient.getProductById(itemRequest.productId());
//
//                PrescriptionItem item = new PrescriptionItem();
//                item.setProductId(itemRequest.productId());
//                item.setQuantity(itemRequest.quantity());
//                item.setDosage(itemRequest.dosage());
//                item.setMedicalRecord(medicalRecord); // Liên kết ngược lại
//                items.add(item);
//            }
//            medicalRecord.setPrescriptionItems(items);
//        }
//
//        return medicalRecordRepository.save(medicalRecord);
//    }

@Transactional
public MedicalRecord createMedicalRecord(MedicalRecordRequest request) {
    // 1. Kiểm tra xem lịch hẹn này đã có bệnh án chưa
    if (medicalRecordRepository.findByAppointmentId(request.getAppointmentId()).isPresent()) {
        throw new IllegalStateException("Medical record for this appointment already exists.");
    }

    MedicalRecord medicalRecord = new MedicalRecord();
    medicalRecord.setAppointmentId(request.getAppointmentId());

    // 2. Lấy thông tin chi nhánh từ appointment-service (cần cho việc trừ kho)
    AppointmentResponseDto appointment = appointmentServiceClient.getAppointmentById(request.getAppointmentId());
    if (appointment.branch() == null || appointment.branch().id() == null) {
        // Tạm thời dùng IllegalStateException, bạn có thể đổi thành AppException
        throw new IllegalStateException("Không thể xác định chi nhánh từ lịch hẹn ID: " + request.getAppointmentId());
    }
    UUID branchId = appointment.branch().id();


    List<PrescriptionItemRequest> itemsToProcess = new ArrayList<>();

    // Áp dụng template hoặc dùng dữ liệu thủ công
    if (request.getTemplateId() != null) {
        // --- TRƯỜNG HỢP DÙNG TEMPLATE ---
        DiagnosisTemplate template = templateRepository.findById(request.getTemplateId())
                .orElseThrow(() -> new RuntimeException("Template not found with id: " + request.getTemplateId()));

        // Sao chép thông tin từ template vào bệnh án
        medicalRecord.setDiagnosis(template.getDiagnosisContent());
        medicalRecord.setIcd10Code(template.getIcd10Code());

        // Sao chép các mục trong đơn thuốc của template để xử lý tiếp
        if (template.getPrescriptionItems() != null) {
            itemsToProcess = template.getPrescriptionItems().stream()
                    .map(item -> new PrescriptionItemRequest(
                            item.getProductId(),
                            item.getQuantity(),
                            item.getDosage()))
                    .collect(Collectors.toList());
        }
    } else {
        // --- TRƯỜNG HỢP TẠO THỦ CÔNG ---
        medicalRecord.setDiagnosis(request.getDiagnosis());
        medicalRecord.setIcd10Code(request.getIcd10Code());
        if (request.getPrescriptionItems() != null) {
            itemsToProcess = request.getPrescriptionItems();
        }
    }

    // 4. Xử lý đơn thuốc (trừ kho và tạo entity)
    if (!itemsToProcess.isEmpty()) {
        List<PrescriptionItem> prescriptionEntities = new ArrayList<>();
        for (PrescriptionItemRequest itemRequest : itemsToProcess) {
            // Gọi sang product-inventory-service để trừ kho
            DeductStockRequest deductRequest = new DeductStockRequest();
            deductRequest.setBranchId(branchId);
            deductRequest.setProductId(itemRequest.productId());
            deductRequest.setQuantityToDeduct(itemRequest.quantity());
            productInventoryClient.deductStock(deductRequest);

            // Tạo entity PrescriptionItem
            PrescriptionItem item = new PrescriptionItem();
            item.setProductId(itemRequest.productId());
            item.setQuantity(itemRequest.quantity());
            item.setDosage(itemRequest.dosage());
            item.setMedicalRecord(medicalRecord); // Liên kết ngược lại
            prescriptionEntities.add(item);
        }
        medicalRecord.setPrescriptionItems(prescriptionEntities);
    }

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
}