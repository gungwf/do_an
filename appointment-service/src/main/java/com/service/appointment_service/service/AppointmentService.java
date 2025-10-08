package com.service.appointment_service.service;

import com.service.appointment_service.client.*;
import com.service.appointment_service.client.ServiceDto;
import com.service.appointment_service.dto.*;
import com.service.appointment_service.entity.Appointment;
import com.service.appointment_service.entity.Enum.AppointmentStatus;
import com.service.appointment_service.exception.AppException;
import com.service.appointment_service.exception.ERROR_CODE;
import com.service.appointment_service.repository.AppointmentRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppointmentService {
    private final AppointmentRepository appointmentRepository;
    private final MedicalServiceClient medicalServiceClient;
    private final UserServiceClient userServiceClient;
    private final ProductInventoryClient productInventoryClient;
    private final EmailService emailService;

    public Appointment createAppointment(String patientEmail,AppointmentRequest request) {
        ServiceDto service = medicalServiceClient.getServiceById(request.getServiceId());
        UserDto patient = userServiceClient.getUserByEmail(patientEmail);
        UserDto doctor = userServiceClient.getUserById(request.getDoctorId());

//        if (!"patient".equalsIgnoreCase(patient.role().toString())) {
//            throw new IllegalArgumentException("User with ID " + patient.id() + " is not a patient.");
//        }
//        if (!"doctor".equalsIgnoreCase(doctor.role().toString())) {
//            throw new IllegalArgumentException("User with ID " + doctor.id() + " is not a doctor.");
//        }

        OffsetDateTime newAppointmentStart = request.getAppointmentTime();
        OffsetDateTime newAppointmentEnd = newAppointmentStart.plusMinutes(service.durationMinutes());

        // check lịch
        List<Appointment> doctorAppointments = appointmentRepository.findByDoctorIdAndAppointmentTimeBetween(
                doctor.id(),
                newAppointmentStart.withHour(0).withMinute(0),
                newAppointmentStart.withHour(23).withMinute(59)
        );

        for (Appointment existingAppointment : doctorAppointments) {
            OffsetDateTime existingStart = existingAppointment.getAppointmentTime();
            OffsetDateTime existingEnd = existingStart.plusMinutes(existingAppointment.getDurationMinutes());
            if (newAppointmentStart.isBefore(existingEnd) && newAppointmentEnd.isAfter(existingStart)) {
                throw new AppException(ERROR_CODE.DOCTOR_BUSY);
            }
        }

        List<Appointment> patientAppointments = appointmentRepository.findByPatientIdAndAppointmentTimeBetween(
                patient.id(),
                newAppointmentStart.withHour(0).withMinute(0),
                newAppointmentStart.withHour(23).withMinute(59)
        );
        for (Appointment existingAppointment : patientAppointments) {
            OffsetDateTime existingStart = existingAppointment.getAppointmentTime();
            OffsetDateTime existingEnd = existingStart.plusMinutes(existingAppointment.getDurationMinutes());
            if (newAppointmentStart.isBefore(existingEnd) && newAppointmentEnd.isAfter(existingStart)) {
                throw new AppException(ERROR_CODE.PATIENT_BUSY);
            }
        }

        // check dị ứng
        String notes = request.getNotes() != null ? request.getNotes() : "";
        try {
            PatientProfileDto profile = userServiceClient.getPatientProfile(patient.id());
            String allergies = profile.allergies() != null ? profile.allergies().toLowerCase() : "";

            if (!allergies.isEmpty() && allergies.contains(service.serviceName().toLowerCase())) {
                notes += " [CẢNH BÁO: Bệnh nhân có thể dị ứng với dịch vụ này!]";
            }
        } catch (Exception e) {
        }

        Appointment appointment = new Appointment();
        appointment.setPatientId(patient.id());
        appointment.setServiceId(service.id());
        appointment.setBranchId(request.getBranchId());
        appointment.setDoctorId(doctor.id());
        appointment.setAppointmentTime(request.getAppointmentTime());
        appointment.setPriceAtBooking(service.price());
        appointment.setDurationMinutes(service.durationMinutes());
        appointment.setNotes(notes);

        Appointment savedAppointment = appointmentRepository.save(appointment);
        AppointmentResponseDto appointmentDto = mapToResponseDto(savedAppointment);
        emailService.sendAppointmentConfirmation(appointmentDto);
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

        try {
            service = medicalServiceClient.getServiceById(appointment.getServiceId());
        } catch (Exception e) {
            throw new AppException(ERROR_CODE.SERVICE_NOT_FOUND);
        }

        PatientDto patientDto = (patient != null) ? new PatientDto(patient.id(), patient.fullName(), patient.email()) : null;
        DoctorDto doctorDto = (doctor != null) ? new DoctorDto(doctor.id(), doctor.fullName()) : null;
        com.service.appointment_service.dto.ServiceDto serviceDto = (service != null) ? new com.service.appointment_service.dto.ServiceDto(service.id(), service.serviceName()) : null;
        BranchDto branchDto = (branch != null) ? new BranchDto(branch.id(), branch.branchName(), branch.address()) : null;

        return new AppointmentResponseDto(
                appointment.getId(),
                appointment.getAppointmentTime(),
                appointment.getDurationMinutes(),
                appointment.getStatus().toString(),
                appointment.getNotes(),
                appointment.getPriceAtBooking(),
                patientDto,
                doctorDto,
                serviceDto,
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

    //  CẬP NHẬT TRẠNG THÁI
    @Transactional
    public AppointmentResponseDto updateAppointmentStatus(UUID appointmentId, String newStatusStr) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new AppException(ERROR_CODE.APPOINTMENT_NOT_FOUND));
        try {
            AppointmentStatus newStatus = AppointmentStatus.valueOf(newStatusStr.toUpperCase());
            appointment.setStatus(newStatus);
            if (newStatus == AppointmentStatus.COMPLETED) {
//                System.out.println("--- DEBUG: BÊN GỌI (appointment-service) ---");
//                System.out.println("Appointment ID: " + appointment.getId());
//                System.out.println("Service ID từ Appointment: " + appointment.getServiceId());
//                System.out.println("Branch ID từ Appointment: " + appointment.getBranchId());

                List<ServiceMaterialDto> materials = medicalServiceClient.getMaterialsForService(appointment.getServiceId());
                // trừ kho
                for (ServiceMaterialDto material : materials) {
                    DeductStockRequest deductRequest = new DeductStockRequest();
                    deductRequest.setBranchId(appointment.getBranchId());
                    deductRequest.setProductId(material.productId());
                    deductRequest.setQuantityToDeduct(material.quantityConsumed());
//                    System.out.println("==> Sắp gọi trừ kho với Product ID: " + material.productId());
//                    System.out.println("==> Sắp gọi trừ kho với Branch ID: " + appointment.getBranchId());

                    productInventoryClient.deductStock(deductRequest);
                }
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
        if (request.getServiceId() != null) {
            ServiceDto newService = medicalServiceClient.getServiceById(request.getServiceId());
            existingAppointment.setServiceId(newService.id());
            existingAppointment.setPriceAtBooking(newService.price());
            existingAppointment.setDurationMinutes(newService.durationMinutes());
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
}