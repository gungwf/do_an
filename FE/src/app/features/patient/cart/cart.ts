import { Component, OnInit, AfterViewInit } from '@angular/core';
import { CartService, CartItem } from '../../../core/services/cartService';
import { ToastrService } from 'ngx-toastr';
import { CommonModule } from '@angular/common'; 
import { FormsModule } from '@angular/forms';
import { CheckoutDialog } from '../../../shared/components/checkout-dialog/checkout-dialog';
import * as AOS from 'aos';
import { RouterModule } from '@angular/router';
@Component({
  selector: 'app-cart',
  standalone: true, 
  templateUrl: './cart.html',
  imports: [
    RouterModule,
    CommonModule,
    FormsModule,
    CheckoutDialog
  ],
  styleUrls: ['./cart.scss'],
})
export class CartComponent implements OnInit {
  cartItems: CartItem[] = [];
  showCheckoutDialog = false;

  constructor(
    private cartService: CartService,
    private toastr: ToastrService
  ) {}

  ngOnInit(): void {
    AOS.init({ once: true });
    this.cartService.cart$.subscribe(items => {
      this.cartItems = items;
    });
  }

  ngAfterViewInit() {
  AOS.refresh();
  }

  updateQuantity(itemId: number | string, quantity: number): void {
    const newQuantity = Math.floor(quantity);
    if (newQuantity < 1) {
      this.removeItem(itemId);
      return;
    }
    this.cartService.updateQuantity(itemId, newQuantity);
    this.toastr.info('Đã cập nhật số lượng');
  }

  removeItem(itemId: number | string): void {
    this.cartService.removeFromCart(itemId);
    this.toastr.success('Đã xóa sản phẩm khỏi giỏ hàng');
  }

  getTotal(): number {
    return this.cartService.getTotalPrice();
  }

  openCheckoutDialog(): void {
    if (this.cartItems.length === 0) {
      this.toastr.warning('Giỏ hàng trống');
      return;
    }
    this.showCheckoutDialog = true;
  }

  closeCheckoutDialog(): void {
    this.showCheckoutDialog = false;
  }
}