import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms'; 
import { ProductService } from '../../../core/services/product';
import { BranchService, Branch } from '../../../core/services/branch.service';

interface InventoryProduct {
  branchId: string;
  productId: string;
  productName: string;
  imageUrl: string;
  quantity: number;
}

@Component({
  selector: 'app-admin-products',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './products.html',
  styleUrl: './products.scss',
})
export class AdminProducts implements OnInit {
  products: InventoryProduct[] = [];
  isLoading = true;
  page = 0;
  size = 10;
  totalPages = 1;
  totalElements = 0;
  branches: Branch[] = [];
  branchId: string = '';

  constructor(
    private productService: ProductService,
    private branchService: BranchService
  ) {}

  ngOnInit(): void {
    this.branchService.getBranches().subscribe({
      next: (data) => {
        this.branches = data;
        if (this.branches.length > 0) {
          this.branchId = this.branches[0].id;
          this.loadProducts();
        } else {
          this.isLoading = false;
        }
      },
      error: () => {
        this.isLoading = false;
      }
    });
  }

  loadProducts(): void {
    if (!this.branchId) {
      this.products = [];
      this.totalPages = 1;
      this.totalElements = 0;
      this.isLoading = false;
      return;
    }
    this.isLoading = true;
    const body = {
      page: this.page,
      size: this.size,
      sortBy: 'lastUpdatedAt',
      sortDir: 'DESC',
      branchId: this.branchId
    };
    this.productService.searchInventoryByBranch(body).subscribe({
      next: res => {
        this.products = res.result.content;
        this.totalPages = res.result.totalPages;
        this.totalElements = res.result.totalElements;
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
        this.products = [];
        this.totalPages = 1;
        this.totalElements = 0;
      }
    });
  }

  onBranchChange() {
    this.page = 0;
    this.loadProducts();
  }

  onPageChange(page: number) {
    if (page >= 0 && page < this.totalPages) {
      this.page = page;
      this.loadProducts();
    }
  }

  deleteProduct(productId: string): void {
    if (confirm('Bạn có chắc muốn xóa sản phẩm này?')) {
      // TODO: Gọi API xóa sản phẩm khỏi kho nếu có
      alert('Đã xóa (mock)');
      this.loadProducts();
    }
  }
}