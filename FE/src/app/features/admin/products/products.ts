import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-admin-products',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './products.html',
  styleUrl: './products.scss',
})
export class AdminProducts implements OnInit {
  products: any[] = [];
  isLoading = true;

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    this.loadProducts();
  }

  loadProducts(): void {
    // TODO: Load products from API
    this.isLoading = false;
  }

  deleteProduct(productId: string): void {
    if (confirm('Bạn có chắc muốn xóa sản phẩm này?')) {
      // TODO: Delete product API call
      console.log('Delete product:', productId);
    }
  }
}








