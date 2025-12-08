import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface MedicalRecordDto {
  id: string;
  appointmentId: string;
  diagnosis: string;
  icd10Code?: string; 
  createdAt?: string;
}

export interface CreateMedicalRecordDto {
  appointmentId: string;
  diagnosis: string;
  icd10Code?: string;
  serviceIds: string[];
  prescriptionItems?: any[];
  templateId?: string | null;
}

export interface StaffAppointmentSearchDto {
  page: number;
  size: number;
  sort?: string;
  startTime?: string;
  endTime?: string;
  searchText?: string;
  status?: string;
}

export interface AppointmentResponseDto {
  id: string;
  appointmentTime?: string;
  status?: string;
  notes?: string;
  priceAtBooking?: number;
  patient?: {
    id: string;
    fullName?: string;
    email?: string;
  };
  doctor?: {
    id: string;
    fullName?: string;
  };
  branch?: {
    id: string;
    branchName?: string;
    address?: string;
  };
}

export interface SpringPage<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
  empty: boolean;
  numberOfElements: number;
  sort: any;
  pageable: any;
}

// ✅ THÊM MỚI - Interfaces cho update medical record với prescription
export interface PrescriptionItem {
  productId: string;
  quantity: number;
  dosage: string;
}

export interface UpdateMedicalRecordRequest {
  diagnosis: string;
  icd10Code: string;
  prescriptionItems: PrescriptionItem[];
  templateId: string | null;
}

export interface MedicalRecordResponse {
  id: string;
  appointmentId: string;
  diagnosis: string;
  prescriptionItems: any[];
  icd10Code: string;
  createdAt: string;
  updatedAt: string;
  performedServices: any[];
  locked: boolean;
  esignature: string | null;
}

@Injectable({ providedIn: 'root' })
export class MedicalRecordService {
  private http = inject(HttpClient);
  private apiUrl = 'http://localhost:8080';

  createMedicalRecord(payload: CreateMedicalRecordDto): Observable<any> {
    return this.http.post(`${this.apiUrl}/medical-records`, payload);
  }

  getStaffAppointments(dto: StaffAppointmentSearchDto): Observable<SpringPage<AppointmentResponseDto>> {
    return this.http.post<SpringPage<AppointmentResponseDto>>(
      `${this.apiUrl}/appointments/staff/appointments`,
      dto
    );
  }

  getMedicalRecordByAppointment(appointmentId: string): Observable<MedicalRecordDto> {
    return this.http.get<MedicalRecordDto>(`${this.apiUrl}/medical-records/appointment/${appointmentId}`);
  }

  // ✅ THÊM MỚI - Method update medical record với prescription items
  updateMedicalRecord(id: string, request: UpdateMedicalRecordRequest): Observable<MedicalRecordResponse> {
    return this.http.put<MedicalRecordResponse>(`${this.apiUrl}/medical-records/${id}`, request);
  }
}