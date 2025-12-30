import { Component, EventEmitter, Input, OnInit, Output, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ServiceService, ServiceSaveRequest, ServiceDto } from '../../../core/services/service.service';
import { ProductService, Product } from '../../../core/services/product';

@Component({
  selector: 'app-service-detail',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './service-detail.html',
  styleUrl: './service-detail.scss'
})
export class ServiceDetail implements OnInit, OnChanges {
  @Input() visible = false;
  @Input() serviceId?: string;
  @Input() mode: 'view' | 'edit' | 'add' = 'view'; 
  
  @Output() closed = new EventEmitter<void>();
  @Output() updated = new EventEmitter<void>();

  // Cấu trúc dữ liệu để gửi lên server
  serviceData: ServiceSaveRequest = this.initEmptyService();

  // Dữ liệu hỗ trợ chọn vật tư tiêu hao (Chỉ lọc SUPPLY)
  supplyProducts: Product[] = []; 
  selectedMaterialId: string = '';
  materialQuantity: number = 1;

  isLoading = false;

  constructor(
    private serviceService: ServiceService,
    private productService: ProductService // Sử dụng ProductService để lấy danh sách SUPPLY
  ) {}

  ngOnInit(): void {
    this.loadSupplyProducts();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['visible']?.currentValue === true) {
      if (this.serviceId) {
        this.loadServiceDetail();
      } else {
        this.serviceData = this.initEmptyService();
      }
    }
  }

  private initEmptyService(): ServiceSaveRequest {
    return {
      serviceName: '',
      description: '',
      price: 0,
      active: true,
      materials: []
    };
  }

  /**
   * Tải toàn bộ sản phẩm và lọc lấy các vật tư (SUPPLY)
   */
  loadSupplyProducts() {
    this.productService.getProducts().subscribe({
      next: (res: Product[]) => {
        // Lọc lấy các sản phẩm có productType là SUPPLY và đang hoạt động
        this.supplyProducts = res.filter(p => p.productType === 'SUPPLY' && p.active !== false);
      },
      error: (err) => console.error('Lỗi tải danh sách sản phẩm:', err)
    });
  }

  /**
   * Lấy chi tiết dịch vụ và danh sách vật tư hiện có
   */
  loadServiceDetail() {
    if (!this.serviceId) return;
    this.isLoading = true;
    this.serviceService.getServiceById(this.serviceId).subscribe({
      next: (res) => {
        this.serviceData = {
          serviceName: res.service.serviceName,
          description: res.service.description,
          price: res.service.price,
          active: res.service.active,
          materials: res.serviceMaterials.map(m => ({
            productId: m.productId,
            productName: m.productName,
            quantityConsumed: m.quantityConsumed
          }))
        };
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Lỗi tải chi tiết dịch vụ:', err);
        this.isLoading = false;
        this.close();
      }
    });
  }

  /**
   * Thêm vật tư vào danh sách tạm thời
   */
  addMaterial() {
    if (!this.selectedMaterialId || this.materialQuantity <= 0) return;
    
    const product = this.supplyProducts.find(p => p.id === this.selectedMaterialId);
    if (!product) return;

    const existing = this.serviceData.materials.find(m => m.productId === this.selectedMaterialId);
    if (existing) {
      existing.quantityConsumed += this.materialQuantity;
    } else {
      this.serviceData.materials.push({
        productId: this.selectedMaterialId,
        productName: product.productName, // Lưu tên để hiển thị trên bảng tạm
        quantityConsumed: this.materialQuantity
      });
    }
    
    this.selectedMaterialId = '';
    this.materialQuantity = 1;
  }

  removeMaterial(index: number) {
    this.serviceData.materials.splice(index, 1);
  }

  save() {
    if (!this.serviceData.serviceName || this.serviceData.price < 0) {
      alert('Vui lòng nhập đầy đủ thông tin bắt buộc!');
      return;
    }

    this.isLoading = true;
    
    const request$ = (this.mode === 'add' || !this.serviceId)
      ? this.serviceService.createService(this.serviceData)
      : this.serviceService.updateService(this.serviceId, this.serviceData);

    request$.subscribe({
      next: () => {
        alert(this.mode === 'add' ? 'Thêm dịch vụ thành công!' : 'Cập nhật dịch vụ thành công!');
        this.updated.emit();
        this.close();
      },
      error: (err) => {
        console.error('Lỗi khi lưu dịch vụ:', err);
        alert('Có lỗi xảy ra, vui lòng thử lại!');
        this.isLoading = false;
      }
    });
  }

  close() {
    this.closed.emit();
  }
}