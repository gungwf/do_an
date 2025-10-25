import { Component, Input, Output, EventEmitter } from '@angular/core';
import { Product } from '../../../core/services/product'; // Import Interface Product
import { CommonModule, CurrencyPipe } from '@angular/common'; // Import CommonModule và CurrencyPipe
import { RouterLink } from '@angular/router'; // Import RouterLink nếu muốn link ảnh/tên

@Component({
  selector: 'app-product-card',
  standalone: true,
  imports: [
    CommonModule, // Cần cho *ngIf, pipe...
    CurrencyPipe, // Để format tiền tệ
    RouterLink    // Nếu muốn dùng routerLink
  ],
  templateUrl: './product-card.html',
  styleUrl: './product-card.scss'
})
export class ProductCard { // Tên class của bạn

  // 1. Input: Nhận dữ liệu sản phẩm từ component cha
  @Input({ required: true }) product!: Product; 
  // '{ required: true }' đảm bảo component cha phải truyền 'product' vào

  // 2. Output: Gửi sự kiện 'addToCart' ra component cha khi nút được nhấn
  @Output() addToCart = new EventEmitter<Product>();

  // 3. Hàm xử lý khi nhấn nút "Chọn mua"
  onAddToCartClick(): void {
    // Gửi (emit) đối tượng 'product' này ra ngoài
    this.addToCart.emit(this.product); 
  }
}