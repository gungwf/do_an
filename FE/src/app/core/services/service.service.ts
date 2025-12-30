import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

// Interface cho vật tư tiêu hao trong dịch vụ
export interface ServiceMaterial {
  productId: string;
  productName?: string; // Có thể có khi nhận từ API chi tiết
  quantityConsumed: number;
}

// Interface cơ bản của Dịch vụ
export interface ServiceDto {
  id: string;
  serviceName: string;
  description: string;
  price: number;
  createdAt?: string;
  updatedAt?: string;
  active: boolean;
}

// Interface Response khi xem chi tiết dịch vụ (Có bọc trong object "service")
export interface ServiceDetailResponse {
  service: ServiceDto;
  serviceMaterials: ServiceMaterial[];
}

// Interface Request khi tạo/sửa dịch vụ
export interface ServiceSaveRequest {
  serviceName: string;
  description: string;
  price: number;
  active: boolean;
  materials: ServiceMaterial[];
}

export interface ServiceSearchRequest {
  page?: number;
  size?: number;
  search?: string;
  sortBy?: string;
  sortDir?: 'asc' | 'desc' | 'ASC' | 'DESC';
}

@Injectable({
  providedIn: 'root'
})
export class ServiceService {
  private apiUrl = 'http://localhost:8080'; 

  constructor(private http: HttpClient) {}

  searchServices(request: ServiceSearchRequest): Observable<ServiceDto[]> {
    const body: ServiceSearchRequest = {
      page: 0,
      size: 10,
      sortBy: 'serviceName',
      sortDir: 'asc',
      ...request 
    };
    return this.http.post<ServiceDto[]>(`${this.apiUrl}/services/search`, body);
  }

  getServices(): Observable<ServiceDto[]> {
    return this.http.get<ServiceDto[]>(`${this.apiUrl}/services`);
  }

  getServiceById(id: string): Observable<ServiceDetailResponse> {
    return this.http.get<ServiceDetailResponse>(`${this.apiUrl}/services/${id}`);
  }

  createService(serviceData: ServiceSaveRequest): Observable<ServiceDto> {
    return this.http.post<ServiceDto>(`${this.apiUrl}/services`, serviceData);
  }

  updateService(id: string, serviceData: ServiceSaveRequest): Observable<ServiceDto> {
    return this.http.put<ServiceDto>(`${this.apiUrl}/services/${id}`, serviceData);
  }

  deleteService(id: string): Observable<any> {
    return this.http.delete(`${this.apiUrl}/services/${id}`);
  }
}