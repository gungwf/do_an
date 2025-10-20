package com.service.appointment_service.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ERROR_CODE {
    UNKNOWN_EXCEPTION(1000, "Lỗi không xác định", HttpStatus.INTERNAL_SERVER_ERROR),
    UNAUTHORIZED(1002, "Bạn không có quyền thực hiện chức năng này", HttpStatus.FORBIDDEN),

    // Lỗi cho user (1xxx)
    USER_NOT_FOUND(1001, "Không tìm thấy người dùng", HttpStatus.NOT_FOUND),
    PATIENT_NOT_FOUND(1003, "Không tìm thấy bệnh nhân", HttpStatus.NOT_FOUND),
    DOCTOR_NOT_FOUND(1004, "Không tìm thấy bác sĩ", HttpStatus.NOT_FOUND),
    BRANCH_NOT_FOUND(1005, "Không tìm thấy cơ sở", HttpStatus.NOT_FOUND),

    // Lỗi cho service (2xxx)
    SERVICE_NOT_FOUND(2001, "Không tìm thấy dịch vụ", HttpStatus.NOT_FOUND),

    // Lỗi cho Appointment (3xxx)
    APPOINTMENT_NOT_FOUND(3001, "Không tìm thấy lịch hẹn", HttpStatus.NOT_FOUND),
    APPOINTMENT_CONFLICT(3002, "Lịch hẹn bị trùng", HttpStatus.BAD_REQUEST),
    APPOINTMENT_NOT_COMPLETED(3003, "Lịch hẹn chưa hoàn thành", HttpStatus.BAD_REQUEST),
    DOCTOR_BUSY(3004,"Doctor is already booked at this time.",HttpStatus.CONFLICT),
    PATIENT_BUSY(3005,"Patient has an appointment at this time.",HttpStatus.CONFLICT),
    INVALID_STATUS(3006,"Invalid status.",HttpStatus.CONFLICT);

    private final int code;
    private final String message;
    private final HttpStatusCode statusCode;

    ERROR_CODE(int code, String message, HttpStatusCode statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }
}