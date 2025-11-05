import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ProductService, Product, ProductSearchResponse } from '../../../core/services/product';
import { ToastrService } from 'ngx-toastr';
import { ProductCard } from '../../../shared/components/product-card/product-card';
import { CartService } from '../../../core/services/cartService'; // ✅ 1. IMPORT CART SERVICE
interface Category {
  id: string;
  name: string;
}

@Component({
  selector: 'app-products',
  standalone: true,
  templateUrl: './products.html',
  styleUrls: ['./products.scss'],
  imports: [CommonModule, ProductCard],
})
export class ProductsComponent implements OnInit {
  products: Product[] = [];
  categories: Category[] = [];
  selectedCategory: string = '';
  sortOrder: string = 'asc';
  page = 0;
  size = 8;
  totalPages = 0;

  constructor(private productService: ProductService, private toastr: ToastrService, private cartService: CartService) {}

  ngOnInit(): void {
    this.loadCategories();
    this.loadProducts();
  }

  loadCategories(): void {
    this.productService.getCategories().subscribe({
      next: (data: Category[]) => {
        console.log('Categories API:', data);
        this.categories = data;
      },
      error: (err) => {
        console.error('Lỗi khi lấy categories:', err);
        this.toastr.error('Không thể tải danh mục sản phẩm', 'Lỗi');
      },
    });
  }

  loadProducts(): void {
    const body = {
      search: '',
      category: this.selectedCategory || null,
      sort: `price,${this.sortOrder}`,
      page: this.page,
      size: this.size,
    };

    this.productService.searchProducts(body).subscribe({
      next: (res: ProductSearchResponse) => {
        this.products = res.content;
        this.totalPages = res.totalPages;
      },
      error: (err) => {
        console.error('Lỗi khi lấy products:', err);
        this.toastr.error('Không thể tải danh sách sản phẩm', 'Lỗi');
      },
    });
  }

  onCategoryChange(event: Event): void {
    const value = (event.target as HTMLSelectElement).value;
    this.selectedCategory = value;
    this.page = 0;
    this.loadProducts();
  }

  onSortChange(event: Event): void {
    const value = (event.target as HTMLSelectElement).value;
    this.sortOrder = value;
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

  onAddToCart(product: Product): void {
    console.log('Thêm vào giỏ hàng:', product);
    
    // Gọi service để thêm sản phẩm
    this.cartService.addToCart(product); 

    this.toastr.success(`${product.productName} đã được thêm vào giỏ!`);
  }
}
