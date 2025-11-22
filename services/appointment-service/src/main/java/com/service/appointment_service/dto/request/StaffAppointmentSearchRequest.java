package com.service.appointment_service.dto.request;

import java.time.LocalDate;
import lombok.Data;

@Data
public class StaffAppointmentSearchRequest {
  private String searchText; // patient name
  private String status; // CONFIRMED, COMPLETED, CANCELED...
  private LocalDate startTime;
  private LocalDate endTime;
  private int page = 0;
  private int size = 10;
  private String sort; // e.g. "appointmentTime,desc"
}
