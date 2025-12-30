import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProductService, Product, ProductSearchResponse } from '../../../core/services/product';
// Đảm bảo đường dẫn import tới ProductDetail đúng với cấu trúc thư mục của bạn
import { ProductDetail } from '../../../shared/components/product-detail/product-detail'; 

@Component({
  selector: 'app-product-management',
  standalone: true,
  imports: [CommonModule, FormsModule, ProductDetail], 
  templateUrl: './product-management.html',
  styleUrl: './product-management.scss',
})
export class ProductManagement implements OnInit {
  // Danh sách sản phẩm và trạng thái phân trang
  products: Product[] = [];
  isLoading = false;
  page = 0;
  size = 12;
  totalPages = 0;
  totalElements = 0;
  searchTerm = '';

  // Quản lý trạng thái Popup (Dùng chung cho cả Xem, Thêm và Sửa)
  showDetail = false;
  selectedProductId?: string;

  constructor(private productService: ProductService) {}

  ngOnInit(): void {
    this.loadProducts();
  }

  /**
   * Tải danh sách sản phẩm từ server dựa trên tìm kiếm và phân trang
   */
  loadProducts(): void {
    this.isLoading = true;
    const body: any = {
      page: this.page,
      size: this.size,
    };
    
    if (this.searchTerm && this.searchTerm.trim() !== '') {
      body.search = this.searchTerm.trim();
    }

    this.productService.searchProducts(body).subscribe({
      next: (res: ProductSearchResponse) => {
        this.products = res.content || [];
        this.totalPages = res.totalPages || 0;
        this.totalElements = res.totalElements || 0;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Lỗi tải danh sách sản phẩm:', err);
        this.products = [];
        this.isLoading = false;
      },
    });
  }

  /**
   * Xử lý khi bấm nút "Thêm sản phẩm"
   * Mở popup với productId = undefined để Component con hiểu là chế độ Thêm mới
   */
  addProduct(): void { 
    this.selectedProductId = undefined; 
    this.showDetail = true;
  }

  /**
   * Xử lý khi bấm nút "Xem chi tiết" hoặc "Sửa"
   */
  viewProduct(product: Product): void {
    this.selectedProductId = product.id; 
    this.showDetail = true;              
  }

  editProduct(product: Product): void { 
    // Tận dụng luồng viewProduct, Component con sẽ có nút "Chỉnh sửa" bên trong
    this.viewProduct(product);
  }

  /**
   * Phản hồi khi Component con báo hiệu đã cập nhật/thêm mới thành công
   */
  onProductUpdated(): void {
    // Tải lại danh sách để đồng bộ dữ liệu mới nhất trên bảng
    this.loadProducts();
  }

  /**
   * Đóng popup và reset ID đã chọn
   */
  closeDetail(): void {
    this.showDetail = false;
    this.selectedProductId = undefined;
  }

  /**
   * Xử lý tìm kiếm
   */
  onSearchChange(): void {
    this.page = 0; // Reset về trang đầu khi tìm kiếm mới
    this.loadProducts();
  }

  /**
   * Điều hướng phân trang
   */
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

  /**
   * Xóa sản phẩm
   */
  deleteProduct(product: Product): void {
    if (confirm(`Bạn có chắc chắn muốn xóa sản phẩm: ${product.productName}?`)) {
      // Ở đây bạn có thể gọi thêm API deleteProduct(product.id) từ ProductService nếu cần
      alert('Chức năng xóa sẽ được thực hiện tại đây');
      // Sau khi xóa thành công: this.loadProducts();
    }
  }
}