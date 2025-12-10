import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProductService, Product, ProductSearchResponse } from '../../../core/services/product';
import { ToastrService } from 'ngx-toastr';
import { ProductCard } from '../../../shared/components/product-card/product-card';
import { CartService } from '../../../core/services/cartService';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import * as AOS from 'aos';

interface Category {
  id: string;
  name: string;
}

@Component({
  selector: 'app-products',
  standalone: true,
  templateUrl: './products.html',
  styleUrls: ['./products.scss'],
  imports: [CommonModule, ProductCard, FormsModule],
})
export class ProductsComponent implements OnInit, OnDestroy {
  // ===== PRODUCTS & CATEGORIES =====
  products: Product[] = [];
  categories: Category[] = [];
  
  // ===== FILTERS =====
  searchTerm: string = '';
  selectedCategory: string = '';
  sortOrder: string = 'asc';
  
  // ===== PAGINATION =====
  page = 0;
  size = 12;
  totalPages = 0;
  totalElements = 0;
  
  // ===== LOADING STATE =====
  isLoading: boolean = false;
  
  // ===== SEARCH DEBOUNCE =====
  private searchSubject = new Subject<string>();

  constructor(
    private productService: ProductService, 
    private toastr: ToastrService, 
    private cartService: CartService
  ) {}

  ngOnInit(): void {
    // ✅ Initialize AOS once with optimized settings
    AOS.init({
      duration: 600,
      easing: 'ease-in-out',
      once: true,
      mirror: false,
      disable: false,
      startEvent: 'DOMContentLoaded'
    });

    this.loadCategories();
    this.loadProducts();
    
    // ✅ Search with debounce to prevent API spam
    this.searchSubject.pipe(
      debounceTime(500),
      distinctUntilChanged()
    ).subscribe(() => {
      this.page = 0;
      this.loadProducts();
    });
  }
  ngAfterViewInit() {
  AOS.refresh();
  }

  ngOnDestroy(): void {
    // ✅ Clean up subscription
    this.searchSubject.complete();
  }

  // ===== LOAD CATEGORIES =====
  loadCategories(): void {
    this.productService.getCategories().subscribe({
      next: (data: Category[]) => {
        console.log('✅ Categories loaded:', data);
        this.categories = data;
      },
      error: (err) => {
        console.error('❌ Error loading categories:', err);
        this.toastr.error('Không thể tải danh mục sản phẩm', 'Lỗi', {
          timeOut: 3000,
          progressBar: true
        });
      },
    });
  }

  // ===== LOAD PRODUCTS =====
  loadProducts(): void {
    this.isLoading = true;

    const body = {
      search: this.searchTerm.trim() || '',
      category: this.selectedCategory || null,
      sort: `price,${this.sortOrder}`,
      page: this.page,
      size: this.size,
    };

    console.log('🔍 Searching products with:', body);

    this.productService.searchProducts(body).subscribe({
      next: (res: ProductSearchResponse) => {
        this.products = res.content || [];
        this.totalPages = res.totalPages || 0;
        this.totalElements = res.totalElements || 0;
        this.isLoading = false;
        
        console.log(`✅ Loaded ${this.products.length} products (Page ${this.page + 1}/${this.totalPages})`);
        
        // ✅ Show message if no products found
        if (this.products.length === 0) {
          this.toastr.info('Không tìm thấy sản phẩm nào', 'Thông báo');
        }
      },
      error: (err) => {
        console.error('❌ Error loading products:', err);
        this.products = [];
        this.isLoading = false;
        this.toastr.error('Không thể tải danh sách sản phẩm', 'Lỗi', {
          timeOut: 3000,
          progressBar: true
        });
      },
    });
  }

  // ===== SEARCH =====
  onSearchChange(): void {
    this.searchSubject.next(this.searchTerm);
  }

  // ✅ NEW: Clear search
  clearSearch(): void {
    this.searchTerm = '';
    this.page = 0;
    this.loadProducts();
  }

  // ===== FILTER BY CATEGORY =====
  onCategoryChange(event: Event): void {
    const value = (event.target as HTMLSelectElement).value;
    this.selectedCategory = value;
    this.page = 0;
    this.loadProducts();
  }

  // ✅ NEW: Filter by category from sidebar
  filterByCategory(categoryId: string): void {
    this.selectedCategory = categoryId;
    this.page = 0;
    this.loadProducts();
  }

  // ===== SORT =====
  onSortChange(event: Event): void {
    const value = (event.target as HTMLSelectElement).value;
    this.sortOrder = value;
    this.page = 0;
    this.loadProducts();
  }

  // ✅ NEW: Change page size
  onSizeChange(): void {
    this.page = 0;
    this.loadProducts();
  }

  // ===== ADD TO CART - OPTIMIZED =====
  onAddToCart(product: Product): void {
    try {
      console.log('🛒 Adding to cart:', product.productName);
      
      // ✅ Add to cart via service
      this.cartService.addToCart(product);

      // ✅ Show immediate toast notification
      this.toastr.success(
        `Đã thêm "${product.productName}" vào giỏ hàng!`, 
        'Thành công', 
        {
          timeOut: 2000,
          progressBar: true,
          positionClass: 'toast-top-right',
          closeButton: true,
          tapToDismiss: true,
          easeTime: 300
        }
      );
    } catch (error) {
      console.error('❌ Error adding to cart:', error);
      this.toastr.error('Không thể thêm sản phẩm vào giỏ hàng', 'Lỗi');
    }
  }

  // ===== PAGINATION =====
  nextPage(): void {
    if (this.page < this.totalPages - 1) {
      this.page++;
      this.loadProducts();
      this.scrollToTop();
    }
  }

  prevPage(): void {
    if (this.page > 0) {
      this.page--;
      this.loadProducts();
      this.scrollToTop();
    }
  }

  goToPage(pageNumber: number): void {
    if (pageNumber >= 0 && pageNumber < this.totalPages) {
      this.page = pageNumber;
      this.loadProducts();
      this.scrollToTop();
    }
  }

  // ✅ ENHANCED: Better pagination with ellipsis support
  getPageNumbers(): number[] {
    const maxPagesToShow = 5;
    const pages: number[] = [];
    
    // If total pages <= maxPagesToShow, show all (except first and last)
    if (this.totalPages <= maxPagesToShow + 2) {
      for (let i = 1; i < this.totalPages - 1; i++) {
        pages.push(i);
      }
      return pages;
    }
    
    // Calculate range around current page
    let startPage = Math.max(1, this.page - 1);
    let endPage = Math.min(this.totalPages - 2, this.page + 1);
    
    // Adjust if at beginning
    if (this.page <= 2) {
      endPage = Math.min(maxPagesToShow - 1, this.totalPages - 2);
      startPage = 1;
    }
    
    // Adjust if at end
    if (this.page >= this.totalPages - 3) {
      startPage = Math.max(1, this.totalPages - maxPagesToShow);
      endPage = this.totalPages - 2;
    }
    
    for (let i = startPage; i <= endPage; i++) {
      pages.push(i);
    }
    
    return pages;
  }

  // ✅ Helper: Check if should show left ellipsis
  get showLeftEllipsis(): boolean {
    const pageNumbers = this.getPageNumbers();
    return pageNumbers.length > 0 && pageNumbers[0] > 1;
  }

  // ✅ Helper: Check if should show right ellipsis
  get showRightEllipsis(): boolean {
    const pageNumbers = this.getPageNumbers();
    return pageNumbers.length > 0 && pageNumbers[pageNumbers.length - 1] < this.totalPages - 2;
  }

  get startIndex(): number {
    return this.page * this.size + 1;
  }

  get endIndex(): number {
    return Math.min((this.page + 1) * this.size, this.totalElements);
  }

  // ===== UTILITIES =====
  private scrollToTop(): void {
    window.scrollTo({ 
      top: 0, 
      behavior: 'smooth' 
    });
  }

  // ===== RESET FILTERS =====
  resetFilters(): void {
    this.searchTerm = '';
    this.selectedCategory = '';
    this.sortOrder = 'asc';
    this.size = 12;
    this.page = 0;
    this.loadProducts();
  }
}