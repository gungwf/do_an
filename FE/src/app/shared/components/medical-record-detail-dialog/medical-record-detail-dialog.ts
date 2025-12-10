import { Component, Input, Output, EventEmitter, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MedicalRecordDetail } from '../../../core/services/medical-record.service';
import { CartService } from '../../../core/services/cartService';
import { ToastrService } from 'ngx-toastr';
import { ProductService } from '../../../core/services/product';
import { firstValueFrom } from 'rxjs';

@Component({
  selector: 'app-medical-record-detail-dialog',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './medical-record-detail-dialog.html',
  styleUrls: ['./medical-record-detail-dialog.scss']
})
export class MedicalRecordDetailDialog {
  @Input() record: MedicalRecordDetail | null = null;
  @Input() open: boolean = false;
  @Output() close = new EventEmitter<void>();

  private cartService = inject(CartService);
  private toastr = inject(ToastrService);
  private productService = inject(ProductService);

  onClose() {
    this.close.emit();
  }

  onBackdrop(event: MouseEvent) {
    this.onClose();
  }

  async addPrescriptionsToCart() {
    if (!this.record?.prescriptionItems?.length) return;

    // Lấy thông tin từng sản phẩm để lấy imageUrl
    const requests = this.record.prescriptionItems.map(item =>
      firstValueFrom(this.productService.getProductById(item.productId))
        .then(product => ({
          id: product.id,
          productName: product.productName,
          price: product.price ?? 0,
          description: product.description ?? '',
          stockQuantity: product.stockQuantity ?? 0,
          image: product.imageUrl ?? '',
          imageUrl: product.imageUrl ?? '',
          categoryName: product.categoryName ?? '',
          categoryId: product.categoryId ?? '',
          quantity: item.quantity
        }))
    );

    const products = await Promise.all(requests);

    for (const prod of products) {
      this.cartService.addToCart(prod, prod.quantity);
    }
    this.toastr.success('Đã thêm toàn bộ đơn thuốc vào giỏ hàng!');
  }
}