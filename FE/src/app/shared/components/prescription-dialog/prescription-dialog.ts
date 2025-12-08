import { Component, EventEmitter, Input, Output, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProductService, Product, ProductSearchResponse } from '../../../core/services/product';
import { MedicalRecordService, PrescriptionItem, UpdateMedicalRecordRequest } from '../../../core/services/medical-record.service';
import { ToastrService } from 'ngx-toastr';

interface SelectedMedicine {
  productId: string;
  productName: string;
  quantity: number;
  dosage: string;
  price: number;
}

@Component({
  selector: 'app-prescription-dialog',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './prescription-dialog.html',
  styleUrls: ['./prescription-dialog.scss']
})
export class PrescriptionDialog implements OnInit {
  @Input() medicalRecordId!: string;
  @Input() appointmentId!: string;
  @Input() patientName: string = '';
  @Input() currentDiagnosis: string = '';
  @Input() currentIcd10Code: string = '';
  @Output() closed = new EventEmitter<void>();
  @Output() saved = new EventEmitter<void>();

  products: Product[] = [];
  categories: any[] = [];
  selectedCategory: string = '';
  searchTerm: string = '';
  page = 0;
  size = 10;
  totalPages = 0;

  selectedMedicines: SelectedMedicine[] = [];

  diagnosis: string = '';
  icd10Code: string = '';

  loading = false;

  constructor(
    private productService: ProductService,
    private medicalRecordService: MedicalRecordService,
    private toastr: ToastrService
  ) {}

  ngOnInit(): void {
    this.diagnosis = this.currentDiagnosis;
    this.icd10Code = this.currentIcd10Code;
    this.loadCategories();
    this.loadProducts();
  }

  loadCategories(): void {
    this.productService.getCategories().subscribe({
      next: (data) => {
        this.categories = data;
      },
      error: (err) => {
        console.error('Error loading categories:', err);
      }
    });
  }

  loadProducts(): void {
    const body = {
      search: this.searchTerm,
      category: this.selectedCategory || null,
      sort: 'productName,asc',
      page: this.page,
      size: this.size
    };

    this.productService.searchProducts(body).subscribe({
      next: (res: ProductSearchResponse) => {
        this.products = res.content;
        this.totalPages = res.totalPages;
      },
      error: (err) => {
        console.error('Error loading products:', err);
        this.toastr.error('Không thể tải danh sách thuốc');
      }
    });
  }

  onSearchChange(): void {
    this.page = 0;
    this.loadProducts();
  }

  onCategoryChange(event: Event): void {
    const value = (event.target as HTMLSelectElement).value;
    this.selectedCategory = value;
    this.page = 0;
    this.loadProducts();
  }

  nextPage(): void {
    if (this.page < this.totalPages - 1) {
      this.page++;
      this.loadProducts();
    }
  }

  prevPage(): void {
    if (this.page > 0) {
      this.page--;
      this.loadProducts();
    }
  }

  addMedicine(product: Product): void {
    const existingIndex = this.selectedMedicines.findIndex(
      item => item.productId === product.id
    );

    if (existingIndex >= 0) {
      this.selectedMedicines[existingIndex].quantity++;
      this.toastr.info(`Đã tăng số lượng ${product.productName}`);
    } else {
      this.selectedMedicines.push({
        productId: product.id,
        productName: product.productName,
        quantity: 1,
        dosage: '',
        price: product.price
      });
      this.toastr.success(`Đã thêm ${product.productName}`);
    }
  }

  removeMedicine(index: number): void {
    const medicine = this.selectedMedicines[index];
    this.selectedMedicines.splice(index, 1);
    this.toastr.info(`Đã xóa ${medicine.productName}`);
  }

  increaseQuantity(index: number): void {
    this.selectedMedicines[index].quantity++;
  }

  decreaseQuantity(index: number): void {
    if (this.selectedMedicines[index].quantity > 1) {
      this.selectedMedicines[index].quantity--;
    }
  }

  getTotalAmount(): number {
    return this.selectedMedicines.reduce(
      (sum, item) => sum + (item.price * item.quantity),
      0
    );
  }

  close(): void {
    this.closed.emit();
  }

  onSubmit(): void {
    if (!this.diagnosis.trim()) {
      this.toastr.warning('Vui lòng nhập chẩn đoán');
      return;
    }

    if (this.selectedMedicines.length === 0) {
      this.toastr.warning('Vui lòng chọn ít nhất một loại thuốc');
      return;
    }

    const missingDosage = this.selectedMedicines.find(item => !item.dosage.trim());
    if (missingDosage) {
      this.toastr.warning(`Vui lòng nhập liều lượng cho ${missingDosage.productName}`);
      return;
    }

    this.loading = true;

    const request: UpdateMedicalRecordRequest = {
      diagnosis: this.diagnosis.trim(),
      icd10Code: this.icd10Code.trim(),
      prescriptionItems: this.selectedMedicines.map(item => ({
        productId: item.productId,
        quantity: item.quantity,
        dosage: item.dosage.trim()
      })),
      templateId: null
    };

    this.medicalRecordService.updateMedicalRecord(this.medicalRecordId, request).subscribe({
      next: (response) => {
        this.toastr.success('Đã lưu đơn thuốc thành công!');
        this.loading = false;
        this.saved.emit();
        this.close();
      },
      error: (err) => {
        console.error('Error updating medical record:', err);
        this.loading = false;
        this.toastr.error(err.error?.message || 'Không thể lưu đơn thuốc');
      }
    });
  }
}