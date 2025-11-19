package com.service.appointment_service.dto.request;

import java.time.LocalDate;
import lombok.Data;

@Data
public class DoctorAppointmentSearchRequest {

  // 1. Tiêu chí lọc (Filtering)
  private String searchText; // Lọc theo tên Bệnh nhân
  private String status; // CONFIRMED, COMPLETED, CANCELED...

  // Thêm lọc theo thời gian
  private LocalDate startTime;
  private LocalDate endTime;

  // 2. Phân trang (Pagination)
  private int page = 0;
  private int size = 10;

  // 3. Sắp xếp (Sorting)
  // Ví dụ: "appointmentTime,desc" hoặc "appointmentTime,asc"
  private String sort;
}