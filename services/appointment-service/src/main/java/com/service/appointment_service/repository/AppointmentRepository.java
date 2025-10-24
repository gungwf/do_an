package com.service.appointment_service.repository;

import com.service.appointment_service.entity.Appointment;
import com.service.appointment_service.entity.Enum.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {
    List<Appointment> findByPatientId(UUID patientId);
    List<Appointment> findByDoctorId(UUID doctorId);
    List<Appointment> findByDoctorIdAndAppointmentTimeBetween(UUID doctorId, OffsetDateTime startTime, OffsetDateTime endTime);
    List<Appointment> findByPatientIdAndAppointmentTimeBetween(UUID patientId, OffsetDateTime startTime, OffsetDateTime endTime);
    List<Appointment> findByStatusAndAppointmentTimeBetween(
            AppointmentStatus status,
            OffsetDateTime start,
            OffsetDateTime end
    );

    boolean existsByDoctorIdAndAppointmentTime(UUID doctorId, OffsetDateTime appointmentTime);
}
