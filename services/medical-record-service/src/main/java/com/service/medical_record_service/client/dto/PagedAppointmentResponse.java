
package com.service.medical_record_service.client.dto;

import java.util.List;

public record PagedAppointmentResponse(List<AppointmentResponseDto> content, int totalPages, long totalElements, int size, int number) {}

