import { Component, OnInit, OnDestroy, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Product, ProductService } from '../../../core/services/product';
import { ToastrService } from 'ngx-toastr';
import { RouterLink, Router } from '@angular/router';
import { ProductCard } from '../../../shared/components/product-card/product-card';
// Import Interface và Dữ liệu từ file mới
import { Testimonial, ALL_TESTIMONIALS } from '../../../core/data/testimonials.data';
import { AuthService } from '../../../core/services/auth';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterLink, ProductCard],
  templateUrl: './home.html',
  styleUrl: './home.scss',
})
export class Home implements OnInit, OnDestroy {
  @ViewChild('productScroller') productScroller!: ElementRef<HTMLDivElement>;

  // --- Logic cho sản phẩm ---
  allProducts: Product[] = [];
  displayedProducts: Product[] = [];
  categories: string[] = ['Tim mạch', 'Hô hấp', 'Tiêu hóa', 'Xương khớp', 'Da liễu', 'Tai mũi họng'];
  selectedCategory: string | null = null;
  isLoading = true;

  // --- LOGIC CHO TESTIMONIAL SLIDER ---
  private testimonialInterval: any;
  currentTestimonialIndex = 0; // Bỏ 'private'
  allTestimonials: Testimonial[] = ALL_TESTIMONIALS;
  displayedTestimonials: Testimonial[] = [];

  constructor(
    private productService: ProductService,
    private toastr: ToastrService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    // Kiểm tra role admin và điều hướng
    if (this.authService.isAuthenticated() && this.authService.isAdmin()) {
      this.router.navigate(['/admin/dashboard']);
      return;
    }

    // Logic tải sản phẩm
    // this.productService.getProducts().subscribe({
    //   next: (products) => {
    //     this.allProducts = products;
    //     if (this.categories.length > 0) {
    //       this.filterByCategory(this.categories[0]);
    //     }
    //     this.isLoading = false;
    //   },
    //   error: (err) => {
    //     console.error('Lỗi tải sản phẩm:', err);
    //     this.toastr.error('Không thể tải danh sách sản phẩm.');
    //     this.isLoading = false;
    //   },
    // });

    // Khởi tạo testimonial slider
    this.updateDisplayedTestimonials();
    this.startTestimonialSlider();
  }

  ngOnDestroy(): void {
    if (this.testimonialInterval) {
      clearInterval(this.testimonialInterval);
    }
  }

  // === CÁC HÀM CHO TESTIMONIAL ===
  startTestimonialSlider(): void {
    if (this.testimonialInterval) {
      clearInterval(this.testimonialInterval);
    }
    this.testimonialInterval = setInterval(() => {
      this.currentTestimonialIndex++;
      if (this.currentTestimonialIndex >= this.allTestimonials.length) {
        this.currentTestimonialIndex = 0;
      }
      this.updateDisplayedTestimonials();
    }, 5000);
  }

  updateDisplayedTestimonials(): void {
    const testimonialsToShow = [];
    for (let i = 0; i < 3; i++) {
      const index = (this.currentTestimonialIndex + i) % this.allTestimonials.length;
      testimonialsToShow.push(this.allTestimonials[index]);
    }
    this.displayedTestimonials = testimonialsToShow;
  }

  /** Xử lý khi người dùng nhấn vào một nút chỉ báo */
  goToTestimonial(index: number): void {
    this.currentTestimonialIndex = index;
    this.updateDisplayedTestimonials();
    this.startTestimonialSlider(); // Khởi động lại timer
  }

  // --- Các hàm cũ ---
  filterByCategory(category: string): void {
    this.selectedCategory = category;
    this.displayedProducts = this.allProducts.filter(
      (p) => p.category === category
    );
    setTimeout(() => {
        if (this.productScroller) {
            this.productScroller.nativeElement.scrollTo({ left: 0, behavior: 'smooth' });
        }
    }, 0);
  }

  scroll(direction: 'left' | 'right'): void {
    if (this.productScroller) {
      const element = this.productScroller.nativeElement;
      const scrollAmount = element.clientWidth * 0.8;
      if (direction === 'left') {
        element.scrollBy({ left: -scrollAmount, behavior: 'smooth' });
      } else {
        element.scrollBy({ left: scrollAmount, behavior: 'smooth' });
      }
    }
  }

  addToCart(product: Product): void {
    this.toastr.success(`Đã thêm "${product.productName}" vào giỏ hàng!`);
  }

  scrollToProducts(): void {
    document.getElementById('products-list')?.scrollIntoView({ behavior: 'smooth' });
  }
}