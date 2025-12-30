import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ServiceService, ServiceDto, ServiceSearchRequest } from '../../../core/services/service.service';
import { ServiceDetail } from '../../../shared/components/service-detail/service-detail';

@Component({
  selector: 'app-service-management',
  standalone: true,
  imports: [CommonModule, FormsModule, ServiceDetail], 
  templateUrl: './service-management.html',
  styleUrl: './service-management.scss',
})
export class ServiceManagement implements OnInit {
  services: ServiceDto[] = [];
  isLoading = false;

  // Quản lý phân trang và tìm kiếm
  searchTerm = '';
  page = 0;
  size = 10;

  // Quản lý trạng thái Popup
  showDetail = false;
  selectedServiceId?: string;
  // Khai báo mode để truyền vào Dialog
  currentMode: 'view' | 'edit' | 'add' = 'view';

  constructor(private serviceService: ServiceService) {}

  ngOnInit(): void {
    this.loadServices();
  }

  loadServices(): void {
    this.isLoading = true;
    const request: ServiceSearchRequest = {
      page: this.page,
      size: this.size,
      search: this.searchTerm.trim(),
      sortBy: 'serviceName',
      sortDir: 'asc'
    };

    this.serviceService.searchServices(request).subscribe({
      next: (res: ServiceDto[]) => {
        this.services = res || [];
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Lỗi khi tải danh sách dịch vụ:', err);
        this.services = [];
        this.isLoading = false;
      }
    });
  }

  onSearchChange(): void {
    this.page = 0;
    this.loadServices();
  }

  /**
   * Chế độ THÊM MỚI
   */
  addService(): void {
    this.selectedServiceId = undefined;
    this.currentMode = 'add';
    this.showDetail = true;
  }

  /**
   * Chế độ XEM CHI TIẾT (Chỉ đọc)
   */
  viewDetail(service: ServiceDto): void {
    this.selectedServiceId = service.id;
    this.currentMode = 'view';
    this.showDetail = true;
  }

  /**
   * Chế độ CHỈNH SỬA (Mở thẳng form sửa)
   */
  editService(service: ServiceDto): void {
    this.selectedServiceId = service.id;
    this.currentMode = 'edit';
    this.showDetail = true;
  }

  closeDetail(): void {
    this.showDetail = false;
    this.selectedServiceId = undefined;
  }

  onServiceUpdated(): void {
    this.loadServices();
  }

  deleteService(service: ServiceDto): void {
    if (confirm(`Bạn có chắc chắn muốn xóa dịch vụ "${service.serviceName}" không?`)) {
      this.serviceService.deleteService(service.id).subscribe({
        next: () => {
          alert('Xóa dịch vụ thành công!');
          this.loadServices();
        },
        error: (err) => {
          console.error('Lỗi khi xóa:', err);
          alert('Có lỗi xảy ra khi xóa dịch vụ.');
        }
      });
    }
  }

  nextPage(): void {
    if (this.services.length === this.size) {
      this.page++;
      this.loadServices();
    }
  }

  prevPage(): void {
    if (this.page > 0) {
      this.page--;
      this.loadServices();
    }
  }
}