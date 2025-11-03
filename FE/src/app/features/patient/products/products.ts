import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { ProductCard } from '../../../shared/components/product-card/product-card';
import { Product } from '../../../core/services/product';
import { CartService } from '../../../core/services/cartService';
import { ToastrService } from 'ngx-toastr';


@Component({
  selector: 'app-products',
  standalone: true,
  imports: [CommonModule, ProductCard],
  templateUrl: './products.html',
  styleUrls: ['./products.scss']
})
export class Products implements OnInit {
  products: Product[] = [];
  readonly apiUrl = 'http://localhost:8080/products';

  constructor(
    private http: HttpClient,
    private cartService: CartService,
    private toastr: ToastrService // ✅ Inject ToastrService
  ) {}

  ngOnInit(): void {
    this.loadProducts();
  }

  loadProducts(): void {
    this.http.get<Product[]>(this.apiUrl).subscribe({
      next: (data) => {
        console.log('✅ Dữ liệu sản phẩm:', data);
        this.products = data;
      },
      error: (err) => {
        console.error('❌ Lỗi khi gọi API sản phẩm:', err);
        this.toastr.error('Không thể tải danh sách sản phẩm', 'Lỗi');
      }
    });
  }

  onAddToCart(product: Product): void {
    console.log('🛒 Sản phẩm được thêm vào giỏ:', product);

    // Gọi service để thêm sản phẩm vào giỏ
    this.cartService.addToCart(product);

    // Hiển thị toastr khi thêm thành công
    this.toastr.success(`${product.productName} đã được thêm vào giỏ hàng!`, 'Thành công'); // ✅

    // In giỏ hàng hiện tại ra console (để kiểm tra)
    // console.log('🧺 Giỏ hàng hiện tại:', this.cartService.getCartItems());
  }
}