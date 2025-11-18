import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

// Interface khớp với API GET /services của bạn
export interface ServiceDto {
  id: string;
  serviceName: string;
  description: string;
  price: number;
  active: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class ServiceService {
  private http = inject(HttpClient);
  // TODO: Cập nhật URL API của bạn (có thể lấy từ file environment)
  private apiUrl = 'http://localhost:8080'; 

  getServices(): Observable<ServiceDto[]> {
    return this.http.get<ServiceDto[]>(`${this.apiUrl}/services`);
  }
}