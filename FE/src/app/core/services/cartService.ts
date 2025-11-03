import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { Product } from './product'; // Giả định Product nằm trong cùng thư mục

/** Cấu trúc của 1 item trong giỏ hàng */
export interface CartItem extends Product {
  quantity: number;
}

@Injectable({
  providedIn: 'root'
})
export class CartService {
  private STORAGE_KEY = 'cart';
  private cartItems: CartItem[] = [];
  private cartSubject = new BehaviorSubject<CartItem[]>([]);
  cart$ = this.cartSubject.asObservable();

  constructor() {
    this.loadCartFromStorage();
  }

  /** 🔄 Đọc dữ liệu từ localStorage */
  private loadCartFromStorage(): void {
    const saved = localStorage.getItem(this.STORAGE_KEY);
    if (saved) {
      try {
        const parsed: CartItem[] = JSON.parse(saved);
        this.cartItems = parsed.map(i => ({ ...i, quantity: i.quantity ?? 1 }));
        this.cartSubject.next(this.getCart()); 
      } catch (error) {
        console.error('❌ Lỗi khi đọc dữ liệu giỏ hàng từ localStorage:', error);
        this.cartItems = [];
        this.cartSubject.next([]);
      }
    }
  }

  /** ✅ Lấy danh sách sản phẩm */
  getCart(): CartItem[] {
    return this.cartItems.map(i => ({ ...i }));
  }

  /** ✅ Thêm sản phẩm */
  addToCart(product: Product, qty = 1): void {
    const existing = this.cartItems.find(p => p.id === product.id);
    if (existing) {
      existing.quantity += qty;
    } else {
      const newItem: CartItem = { ...product, quantity: qty };
      this.cartItems.push(newItem);
    }
    this.saveCart();
  }

  /** ✅ Cập nhật số lượng */
  updateQuantity(productId: number | string, quantity: number): void {
    const item = this.cartItems.find(p => p.id === productId);
    if (!item) return;

    // Đảm bảo số lượng là số nguyên dương
    const newQuantity = Math.floor(quantity);

    if (newQuantity < 1) {
      this.removeFromCart(productId);
      return;
    }

    item.quantity = newQuantity;
    this.saveCart();
  }

  /** ✅ Xóa sản phẩm */
  removeFromCart(productId: number | string): void {
    this.cartItems = this.cartItems.filter(p => p.id !== productId);
    this.saveCart();
  }

  /** ✅ Xóa toàn bộ */
  clearCart(): void {
    this.cartItems = [];
    this.saveCart();
  }

  /** 💰 Tổng giá trị */
  getTotalPrice(): number {
    return this.cartItems.reduce((s, i) => s + (i.price || 0) * i.quantity, 0);
  }

  /** 🔢 TỔNG SỐ LƯỢNG SẢN PHẨM KHÁC NHAU (FIX LỖI) */
  getTotalQuantity(): number {
    return this.cartItems.reduce((sum, item) => sum + item.quantity, 0);
  }

  /** 💾 Lưu localStorage + thông báo */
  private saveCart(): void {
    localStorage.setItem(this.STORAGE_KEY, JSON.stringify(this.cartItems));
    this.cartSubject.next(this.getCart());
  }

  /** ✅ Dọn dẹp storage (khi logout) */
  clearStorage(): void {
    localStorage.removeItem(this.STORAGE_KEY);
    this.cartItems = [];
    this.cartSubject.next([]);
  }
}