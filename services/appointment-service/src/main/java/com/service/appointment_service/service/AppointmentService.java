package com.service.appointment_service.service;

import com.service.appointment_service.client.client.MedicalServiceClient;
import com.service.appointment_service.client.client.ProductInventoryClient;
import com.service.appointment_service.client.client.ServiceClient;
import com.service.appointment_service.client.client.UserServiceClient;
import com.service.appointment_service.client.dto.*;
import com.service.appointment_service.client.dto.ServiceDto;
import com.service.appointment_service.dto.request.AppointmentRequest;
import com.service.appointment_service.dto.response.AppointmentResponseDto;
import com.service.appointment_service.dto.response.BranchDto;
import com.service.appointment_service.dto.response.DoctorDto;
import com.service.appointment_service.dto.response.PatientDto;
import com.service.appointment_service.entity.Appointment;
import com.service.appointment_service.entity.Enum.AppointmentStatus;
import com.service.appointment_service.entity.Enum.ProtocolStatus;
import com.service.appointment_service.exception.AppException;
import com.service.appointment_service.exception.ERROR_CODE;
import com.service.appointment_service.repository.AppointmentRepository;
import com.service.appointment_service.repository.ProtocolTrackingRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentService {
    private final AppointmentRepository appointmentRepository;
    private final MedicalServiceClient medicalServiceClient;
    private final UserServiceClient userServiceClient;
    private final ProductInventoryClient productInventoryClient;
    private final EmailService emailService;
    private final ProtocolTrackingRepository protocolTrackingRepository;
    private final PaymentService paymentService;
    private final ServiceClient serviceClient;

// ko thanh toán
    public Appointment createAppointment(String patientEmail, AppointmentRequest request) {
        UserDto patient = userServiceClient.getUserByEmail(patientEmail);
        UserDto doctor = userServiceClient.getUserById(request.getDoctorId());

        // check lịch
        boolean isSlotBooked = appointmentRepository
                .existsByDoctorIdAndAppointmentTime(request.getDoctorId(), request.getAppointmentTime());

        if (isSlotBooked) {
            throw new IllegalStateException("Lịch hẹn này đã có người đặt. Vui lòng chọn thời gian khác.");
        }

        Appointment appointment = new Appointment();
        appointment.setPatientId(patient.id());
        appointment.setBranchId(request.getBranchId());
        appointment.setDoctorId(doctor.id());
        appointment.setAppointmentTime(request.getAppointmentTime());
        appointment.setServiceId(null);
        appointment.setPriceAtBooking(new BigDecimal("150000"));
        appointment.setStatus(AppointmentStatus.PENDING);
        appointment.setNotes(request.getNotes());

        Appointment savedAppointment = appointmentRepository.save(appointment);
        return savedAppointment;
    }

    public AppointmentResponseDto getAppointmentById(UUID id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new AppException(ERROR_CODE.APPOINTMENT_NOT_FOUND));
        return mapToResponseDto(appointment);
    }

    public AppointmentResponseDto mapToResponseDto(Appointment appointment) {
        UserDto patient = null;
        UserDto doctor = null;
        BranchDto branch = null;
        ServiceDto service = null;

        try {
            patient = userServiceClient.getUserById(appointment.getPatientId());
        } catch (Exception e) {
            throw new AppException(ERROR_CODE.PATIENT_NOT_FOUND);
        }

        if (appointment.getDoctorId() != null) {
            try {
                doctor = userServiceClient.getUserById(appointment.getDoctorId());
            } catch (Exception e) {
                throw new AppException(ERROR_CODE.DOCTOR_NOT_FOUND);
            }
        }

        try {
            branch = userServiceClient.getBranchById(appointment.getBranchId());
        } catch (Exception e) {
            throw new AppException(ERROR_CODE.BRANCH_NOT_FOUND);
        }


        PatientDto patientDto = (patient != null) ? new PatientDto(patient.id(), patient.fullName(), patient.email()) : null;
        DoctorDto doctorDto = (doctor != null) ? new DoctorDto(doctor.id(), doctor.fullName()) : null;
        BranchDto branchDto = (branch != null) ? new BranchDto(branch.id(), branch.branchName(), branch.address()) : null;

        return new AppointmentResponseDto(
                appointment.getId(),
                appointment.getAppointmentTime(),
                appointment.getStatus().toString(),
                appointment.getNotes(),
                appointment.getPriceAtBooking(),
                patientDto,
                doctorDto,
                branchDto
        );
    }

    public List<AppointmentResponseDto> getAppointmentsForPatient(UUID patientId) {
        List<Appointment> appointments = appointmentRepository.findByPatientId(patientId);

        return appointments.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    public List<AppointmentResponseDto> getAppointmentsForDoctor(UUID doctorId) {
        List<Appointment> appointments = appointmentRepository.findByDoctorId(doctorId);
        return appointments.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    // Confirm lịch
    @Transactional
    public AppointmentResponseDto updateAppointmentStatus(UUID appointmentId, String newStatusStr) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new AppException(ERROR_CODE.APPOINTMENT_NOT_FOUND));
        try {
            AppointmentStatus newStatus = AppointmentStatus.valueOf(newStatusStr.toUpperCase());
            appointment.setStatus(newStatus);
            if (newStatus == AppointmentStatus.COMPLETED) {
                List<ServiceMaterialDto> materials = medicalServiceClient.getMaterialsForService(appointment.getServiceId());
                // trừ kho
                for (ServiceMaterialDto material : materials) {
                    DeductStockRequest deductRequest = new DeductStockRequest();
                    deductRequest.setBranchId(appointment.getBranchId());
                    deductRequest.setProductId(material.productId());
                    deductRequest.setQuantityToDeduct(material.quantityConsumed());
                    productInventoryClient.deductStock(deductRequest);
                }
                protocolTrackingRepository
                        .findByPatientIdAndProtocolServiceIdAndStatus(
                                appointment.getPatientId(),
                                appointment.getServiceId(),
                                ProtocolStatus.IN_PROGRESS)
                        .ifPresent(protocol -> { // ifPresent: Chỉ thực hiện nếu tìm thấy

                            log.info("--- Cập nhật tiến độ cho liệu trình ID: {} ---", protocol.getId());

                            // Tăng số buổi đã hoàn thành
                            protocol.setCompletedSessions(protocol.getCompletedSessions() + 1);

                            // Nếu đã đủ số buổi, đánh dấu liệu trình là hoàn thành
                            if (protocol.getCompletedSessions() >= protocol.getTotalSessions()) {
                                protocol.setStatus(ProtocolStatus.COMPLETED);
                                log.info("--- Liệu trình ID: {} đã hoàn thành! ---", protocol.getId());
                            }
                            protocolTrackingRepository.save(protocol);
                        });
            }
            try {
                AppointmentResponseDto dto = mapToResponseDto(appointment);
                emailService.sendAppointmentCompletedEmail(dto);
            } catch (Exception e) {
                log.error("Lỗi khi gửi email hoàn thành lịch hẹn: {}", e.getMessage());
            }

            try {
                // 10,000 VNĐ = 1 điểm
                int pointsToAdd = appointment.getPriceAtBooking().intValue() / 10000;
                if (pointsToAdd > 0) {
                    AddPointsRequest pointsRequest = new AddPointsRequest();
                    pointsRequest.setPointsToAdd(pointsToAdd);
                    userServiceClient.addPointsToPatient(appointment.getPatientId(), pointsRequest);
                    log.info("Đã cộng {} điểm cho bệnh nhân ID: {}", pointsToAdd, appointment.getPatientId());
                }
            } catch (Exception e) {
                log.error("Lỗi khi cộng điểm cho bệnh nhân {}: {}", appointment.getPatientId(), e.getMessage());
            }

        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid status: " + newStatusStr);
        }
        Appointment updatedAppointment = appointmentRepository.save(appointment);
        return mapToResponseDto(updatedAppointment);
    }

    // CẬP NHẬT LỊCH HẸN
    @Transactional
    public AppointmentResponseDto updateAppointment(UUID appointmentId, AppointmentRequest request) {
        Appointment existingAppointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new AppException(ERROR_CODE.APPOINTMENT_NOT_FOUND));
        // (Thêm logic kiểm tra trùng lịch mới ở đây nếu cần, tương tự như hàm createAppointment)
        if (request.getDoctorId() != null) {
            existingAppointment.setDoctorId(request.getDoctorId());
        }

        if (request.getAppointmentTime() != null) {
            existingAppointment.setAppointmentTime(request.getAppointmentTime());
        }
        if (request.getNotes() != null) {
            existingAppointment.setNotes(request.getNotes());
        }

        Appointment updatedAppointment = appointmentRepository.save(existingAppointment);

        return mapToResponseDto(updatedAppointment);
    }

    // LẤY TẤT CẢ LỊCH HẸN
    public List<AppointmentResponseDto> getAllAppointments() {
        return appointmentRepository.findAll()
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    public List<AppointmentResponseDto> findAppointmentsForReminder() {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime in24Hours = now.plusHours(24);

        List<Appointment> appointments = appointmentRepository.findByStatusAndAppointmentTimeBetween(
                AppointmentStatus.CONFIRMED,
                now,
                in24Hours
        );

        return appointments.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public AppointmentResponseDto cancelAppointment(UUID appointmentId, UUID patientId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new AppException(ERROR_CODE.APPOINTMENT_NOT_FOUND));

        if (!appointment.getPatientId().equals(patientId)) {
            throw new AppException(ERROR_CODE.UNAUTHORIZED);
        }

        if (appointment.getStatus() == AppointmentStatus.COMPLETED || appointment.getStatus() == AppointmentStatus.CANCELED) {
            throw new AppException(ERROR_CODE.INVALID_STATUS);
        }
        //chính sách huỷ hẹn
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime appointmentTime = appointment.getAppointmentTime();

        // Tính khoảng thời gian (tính bằng giờ) từ lúc hủy đến giờ hẹn
        long hoursUntilAppointment = Duration.between(now, appointmentTime).toHours();

        int refundPercentage = 0;

        // Hủy trước 24 giờ
        if (hoursUntilAppointment >= 24) {
            refundPercentage = 100; // Hoàn 100%
        }
        // Hủy trong vòng 24 giờ
        else {
            refundPercentage = 0;
        }

        // (Trong thực tế, bạn sẽ dùng refundPercentage để gọi API thanh toán và hoàn tiền)
        log.info("Hủy lịch hẹn ID: {}. Thời gian còn lại: {} giờ. Tỷ lệ hoàn tiền: {}%",
                appointmentId, hoursUntilAppointment, refundPercentage);

        appointment.setStatus(AppointmentStatus.CANCELED);
        Appointment updatedAppointment = appointmentRepository.save(appointment);
        AppointmentResponseDto appointmentResponseDto = mapToResponseDto(updatedAppointment);
        emailService.sendAppointmentCancellation(appointmentResponseDto);
        return mapToResponseDto(updatedAppointment);
    }

    @Transactional
    public void confirmAppointmentPayment(String orderId) {
        // 1. Tìm lịch hẹn bằng orderId (chính là appointmentId)
        Appointment appointment = appointmentRepository.findById(UUID.fromString(orderId))
                .orElseThrow(() -> new RuntimeException("Appointment not found with id: " + orderId)); // Sau này có thể đổi thành AppException

        // 2. Cập nhật trạng thái thành CONFIRMED
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        appointmentRepository.save(appointment);

        log.info("Đã xác nhận thanh toán và cập nhật trạng thái cho Lịch hẹn ID: {}", orderId);

        // 3. Gửi email xác nhận lịch hẹn thành công
        AppointmentResponseDto dto = mapToResponseDto(appointment);
        emailService.sendAppointmentConfirmation(dto);
    }

    public Appointment findById(UUID id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));
    }

    public BigDecimal getServicePrice(UUID serviceId) {
        return serviceClient.getServicePrice(serviceId); // Gọi REST API
    }

    public Appointment save(Appointment appointment) {
        return appointmentRepository.save(appointment);
    }
}