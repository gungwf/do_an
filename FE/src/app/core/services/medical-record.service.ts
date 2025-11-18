import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

// Interface cho payload gửi đi
export interface CreateMedicalRecordDto {
  appointmentId: string;
  diagnosis: string;
  serviceIds: string[];
  prescriptionItems: any[]; // Gửi mảng rỗng []
}

@Injectable({
  providedIn: 'root'
})
export class MedicalRecordService {
  private http = inject(HttpClient);
  private apiUrl = 'http://localhost:8080';

  createMedicalRecord(payload: CreateMedicalRecordDto): Observable<any> {
    return this.http.post(`${this.apiUrl}/medical-records`, payload);
  }
}