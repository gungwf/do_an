import { Component, Input, Output, EventEmitter, OnChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProductService, Product } from '../../../core/services/product';

@Component({
  selector: 'app-product-detail',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './product-detail.html',
  styleUrl: './product-detail.scss',
})
export class ProductDetail implements OnChanges {
  @Input() productId?: string;
  @Input() visible = false;
  @Output() closed = new EventEmitter<void>();
  @Output() updated = new EventEmitter<void>();

  product: Product = this.getEmptyProduct();
  isLoading = false;
  isEditing = false;
  selectedFile: File | null = null; // Lưu file ảnh khi tạo mới

  constructor(private productService: ProductService) {}

  ngOnChanges() {
    if (this.visible) {
      if (this.productId) {
        this.isEditing = false;
        this.fetchProduct();
      } else {
        // Chế độ thêm mới
        this.isEditing = true;
        this.product = this.getEmptyProduct();
        this.selectedFile = null;
      }
    }
  }

  getEmptyProduct(): Product {
    return {
      id: '',
      productName: '',
      description: '',
      price: 0,
      productType: 'MEDICINE',
      category: '',
      imageUrl: '',
      active: true
    };
  }

  fetchProduct() {
    this.isLoading = true;
    this.productService.getProductById(this.productId!).subscribe({
      next: (res) => { this.product = res; this.isLoading = false; },
      error: () => { this.isLoading = false; this.close(); }
    });
  }

  onFileSelected(event: any) {
    const file: File = event.target.files[0];
    if (!file) return;

    if (this.productId) {
      // Nếu đang ở chế độ Sửa: Upload ngay lập tức
      this.isLoading = true;
      this.productService.uploadProductImage(this.productId, file).subscribe({
        next: () => { this.fetchProduct(); this.updated.emit(); alert('Cập nhật ảnh thành công!'); },
        error: () => { this.isLoading = false; alert('Lỗi tải ảnh!'); }
      });
    } else {
      // Nếu đang ở chế độ Thêm mới: Chỉ lưu file lại để đợi nhấn "Lưu"
      this.selectedFile = file;
      const reader = new FileReader();
      reader.onload = (e: any) => this.product.imageUrl = e.target.result;
      reader.readAsDataURL(file);
    }
  }

  saveProduct() {
    if (!this.product.productName) { alert('Vui lòng nhập tên sản phẩm'); return; }
    this.isLoading = true;

    if (this.productId) {
      // LOGIC CẬP NHẬT (PUT)
      this.productService.updateProduct(this.productId, this.product).subscribe({
        next: () => this.onSaveSuccess('Cập nhật thành công!'),
        error: () => { this.isLoading = false; alert('Lỗi cập nhật!'); }
      });
    } else {
      // LOGIC THÊM MỚI (POST)
      this.productService.createProduct(this.product).subscribe({
        next: (res) => {
          if (this.selectedFile) {
            this.productService.uploadProductImage(res.id, this.selectedFile).subscribe({
              next: () => this.onSaveSuccess('Thêm sản phẩm và ảnh thành công!'),
              error: () => this.onSaveSuccess('Đã thêm sản phẩm nhưng lỗi tải ảnh!')
            });
          } else {
            this.onSaveSuccess('Thêm sản phẩm thành công!');
          }
        },
        error: () => { this.isLoading = false; alert('Lỗi khi tạo sản phẩm!'); }
      });
    }
  }

  onSaveSuccess(msg: string) {
    this.isLoading = false;
    this.isEditing = false;
    alert(msg);
    this.updated.emit();
    this.close();
  }

  toggleEdit() {
    if (this.productId) {
      this.isEditing = !this.isEditing;
      if (!this.isEditing) this.fetchProduct();
    } else {
      this.close();
    }
  }

  close() { this.closed.emit(); }
}