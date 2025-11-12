import { Component, OnInit } from '@angular/core';
import { CartService, CartItem } from '../../../core/services/cartService'; // Điều chỉnh lại đường dẫn
import { ToastrService } from 'ngx-toastr';
import { CommonModule } from '@angular/common'; 
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router'; // Thường cần trong các component feature

@Component({
  selector: 'app-cart',
  // THÊM standalone: true để sử dụng imports: []
  standalone: true, 
  templateUrl: './cart.html',
  imports: [
    CommonModule, // Cho *ngIf, *ngFor, currency pipe
    FormsModule,  // Cho [(ngModel)]
    RouterLink,   // Giả sử có link bên trong
  ],
  styleUrls: ['./cart.scss'],
})
export class CartComponent implements OnInit {
  cartItems: CartItem[] = [];

  constructor(
    private cartService: CartService,
    private toastr: ToastrService
  ) {}

  ngOnInit(): void {
    // Không cần loadCart() vì cart$.subscribe sẽ chạy ngay khi khởi tạo
    
    // Lắng nghe thay đổi giỏ hàng để cập nhật realtime
    this.cartService.cart$.subscribe(items => {
      this.cartItems = items;
    });
  }

  // /** 🔄 Tải dữ liệu từ service (Đã bỏ vì đã dùng subscribe) */
  // loadCart(): void {
  //   this.cartItems = this.cartService.getCart();
  // }

  /** ✏️ Cập nhật số lượng */
  updateQuantity(itemId: number | string, quantity: number): void {
    // Ép kiểu quantity thành số nguyên
    const newQuantity = Math.floor(quantity);

    if (newQuantity < 1) {
      // Nếu người dùng nhập 0 hoặc số âm, xử lý xóa sản phẩm
      this.removeItem(itemId);
      return;
    }
    
    this.cartService.updateQuantity(itemId, newQuantity);
    this.toastr.info('Đã cập nhật số lượng');
  }

  /** ❌ Xóa sản phẩm (ĐÃ ĐỔI TÊN HÀM) */
  removeItem(itemId: number | string): void {
    this.cartService.removeFromCart(itemId);
    this.toastr.success('Đã xóa sản phẩm khỏi giỏ hàng');
  }

  /** 💰 Tính tổng tiền */
  getTotal(): number {
    return this.cartService.getTotalPrice();
  }
}