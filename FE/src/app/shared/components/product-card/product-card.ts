import { Component, Input, Output, EventEmitter } from '@angular/core';
import { Product } from '../../../core/services/product';
import { CommonModule, CurrencyPipe } from '@angular/common';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-product-card',
  standalone: true,
  imports: [
    CommonModule,
    CurrencyPipe,
    RouterLink
  ],
  templateUrl: './product-card.html',
  styleUrl: './product-card.scss'
})
export class ProductCard {
  // ✅ Input: Receive product data from parent
  @Input({ required: true }) product!: Product;

  // ✅ Output: Emit addToCart event to parent
  @Output() addToCart = new EventEmitter<Product>();

  // ✅ Simplified click handler - just emit, no logic
  onAddToCartClick(): void {
    console.log('🛒 Product card emitting:', this.product.productName);
    this.addToCart.emit(this.product);
  }

  // ✅ Image error handler
  onImageError(event: any): void {
    console.warn('⚠️ Image failed to load:', this.product.productName);
    event.target.src = 'assets/images/default-product.png';
  }

  // ✅ Helper: Get display image
  get displayImage(): string {
    return this.product.imageUrl || this.product.image || 'assets/images/default-product.png';
  }

  // ✅ Helper: Format price with VND
  get formattedPrice(): string {
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND'
    }).format(this.product.price);
  }
  
}