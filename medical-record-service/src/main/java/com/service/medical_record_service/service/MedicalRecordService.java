package com.service.medical_record_service.service;

import com.service.medical_record_service.client.*;
import com.service.medical_record_service.dto.MedicalRecordRequest;
import com.service.medical_record_service.dto.PrescriptionItemRequest;
import com.service.medical_record_service.entity.MedicalRecord;
import com.service.medical_record_service.entity.PrescriptionItem;
import com.service.medical_record_service.exception.AppException;
import com.service.medical_record_service.exception.ERROR_CODE;
import com.service.medical_record_service.repository.MedicalRecordRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MedicalRecordService {
    private final MedicalRecordRepository medicalRecordRepository;
    private final ProductInventoryClient productInventoryClient;
    private final AppointmentServiceClient appointmentServiceClient;

    @Transactional
    public MedicalRecord createMedicalRecord(MedicalRecordRequest request) {
        if(medicalRecordRepository.findByAppointmentId(request.getAppointmentId()).isPresent()) {
            throw new AppException(ERROR_CODE.DUPLICATE_MEDICAL_RECORD);
        }

        AppointmentResponseDto appointment = appointmentServiceClient.getAppointmentById(request.getAppointmentId());
        UUID branchId = appointment.branch().id();

        System.out.println("--- DEBUG: BÊN GỌI (medical-record-service) ---");
        System.out.println("Đang xử lý cho Appointment ID: " + request.getAppointmentId());
        System.out.println("Đã lấy được Branch ID từ Appointment: " + branchId);

        if (request.getPrescriptionItems() != null && !request.getPrescriptionItems().isEmpty()) {
            for (PrescriptionItemRequest itemRequest : request.getPrescriptionItems()) {
                // Tạo request để gọi sang product-inventory-service
                ProductDto product = productInventoryClient.getProductById(itemRequest.productId());

                // 2. Kiểm tra xem có phải là thuốc không
                if (product.productType() != ProductType.MEDICINE) {
                    throw new AppException(ERROR_CODE.INVALID_PRODUCT_TYPE_FOR_PRESCRIPTION);
                }
                DeductStockRequest deductRequest = new DeductStockRequest();
                deductRequest.setBranchId(branchId);
                deductRequest.setProductId(itemRequest.productId());
                deductRequest.setQuantityToDeduct(itemRequest.quantity());
//
                System.out.println("==> Sắp gọi trừ kho với Product ID: " + itemRequest.productId());
                System.out.println("==> Sắp gọi trừ kho với Branch ID: " + branchId);

                // Gọi API trừ kho. Nếu hết hàng, nó sẽ ném exception và dừng toàn bộ hàm này.
                productInventoryClient.deductStock(deductRequest);
            }
        }

        MedicalRecord medicalRecord = new MedicalRecord();
        medicalRecord.setAppointmentId(request.getAppointmentId());
        medicalRecord.setDiagnosis(request.getDiagnosis());
        medicalRecord.setIcd10Code(request.getIcd10Code());
        if (request.getPrescriptionItems() != null && !request.getPrescriptionItems().isEmpty()) {
            List<PrescriptionItem> items = new ArrayList<>();
            for (PrescriptionItemRequest itemRequest : request.getPrescriptionItems()) {
                // Gọi sang product service để xác thực sản phẩm
                productInventoryClient.getProductById(itemRequest.productId());

                PrescriptionItem item = new PrescriptionItem();
                item.setProductId(itemRequest.productId());
                item.setQuantity(itemRequest.quantity());
                item.setDosage(itemRequest.dosage());
                item.setMedicalRecord(medicalRecord); // Liên kết ngược lại
                items.add(item);
            }
            medicalRecord.setPrescriptionItems(items);
        }

        return medicalRecordRepository.save(medicalRecord);
    }

    public MedicalRecord getRecordByAppointmentId(UUID appointmentId) {
        return medicalRecordRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new AppException(ERROR_CODE.PRODUCT_NOT_FOUND));
    }

    public MedicalRecord lockMedicalRecord(UUID recordId, String signatureData) {
        // 1. Tìm bệnh án
        MedicalRecord medicalRecord = medicalRecordRepository.findById(recordId)
                .orElseThrow(() -> new AppException(ERROR_CODE.MEDICAL_RECORD_NOT_FOUND)); // Cần thêm mã lỗi này

        // 2. Kiểm tra xem bệnh án đã bị khóa chưa
        if (medicalRecord.isLocked()) {
            throw new IllegalStateException("Medical record is already locked.");
        }

        // 3. Lưu chữ ký và đặt cờ khóa
        medicalRecord.setESignature(signatureData); // signatureData có thể là tên bác sĩ, hoặc một chuỗi base64 của hình ảnh chữ ký
        medicalRecord.setLocked(true);

        // 4. Lưu lại
        return medicalRecordRepository.save(medicalRecord);
    }
}