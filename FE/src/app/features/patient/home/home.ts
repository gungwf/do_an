import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Product, ProductService, ProductSearchResponse } from '../../../core/services/product';
import { ToastrService } from 'ngx-toastr';
import { RouterLink, Router } from '@angular/router';
import { ProductCard } from '../../../shared/components/product-card/product-card';
import { Testimonial, ALL_TESTIMONIALS } from '../../../core/data/testimonials.data';
import { AuthService } from '../../../core/services/auth';
import { CartService } from '../../../core/services/cartService';

import * as AOS from 'aos';

interface Category {
  id: string;
  name: string;
}

interface ContactFormData {
  name: string;
  email: string;
  subject: string;
  message: string;
}

interface FaqItem {
  question: string;
  answer: string;
  isOpen: boolean;
}

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterLink, ProductCard, FormsModule],
  templateUrl: './home.html',
  styleUrl: './home.scss',
})
export class Home implements OnInit, OnDestroy {
  // ===== PRODUCTS & CATEGORIES ===== (Keep for future use)
  displayedProducts: Product[] = [];
  allCategories: Category[] = [];
  categories: Category[] = [];
  selectedCategory: string = '';
  isLoading = false; // ✅ Changed to false

  // ===== TESTIMONIALS =====
  private testimonialInterval: any;
  currentTestimonialIndex = 0;
  allTestimonials: Testimonial[] = ALL_TESTIMONIALS;
  displayedTestimonials: Testimonial[] = [];

  // ===== CONTACT FORM =====
  contactData: ContactFormData = {
    name: '',
    email: '',
    subject: '',
    message: ''
  };
  isSubmittingContact = false;

  // ===== FAQ =====
  activeFaqTab: 'general' | 'pricing' | 'support' = 'general';
  
  generalFaqs: FaqItem[] = [
    {
      question: 'Làm thế nào để đặt thuốc trên website?',
      answer: 'Bạn có thể dễ dàng đặt thuốc bằng cách tìm kiếm sản phẩm, thêm vào giỏ hàng và tiến hành thanh toán. Chúng tôi hỗ trợ nhiều hình thức thanh toán an toàn.',
      isOpen: true
    },
    {
      question: 'Thời gian giao hàng là bao lâu?',
      answer: 'Thời gian giao hàng tiêu chuẩn là 2-3 ngày làm việc đối với nội thành và 4-7 ngày đối với các tỉnh thành khác. Chúng tôi cũng có dịch vụ giao hàng nhanh trong 24h.',
      isOpen: false
    },
    {
      question: 'Tôi có thể trả lại sản phẩm không?',
      answer: 'Bạn có thể trả lại sản phẩm trong vòng 7 ngày nếu sản phẩm còn nguyên vẹn, chưa qua sử dụng và có hóa đơn mua hàng. Vui lòng liên hệ bộ phận chăm sóc khách hàng để được hỗ trợ.',
      isOpen: false
    }
  ];

  pricingFaqs: FaqItem[] = [
    {
      question: 'Có chương trình giảm giá nào không?',
      answer: 'Chúng tôi thường xuyên có các chương trình khuyến mãi, giảm giá cho khách hàng thân thiết và các dịp đặc biệt. Đăng ký nhận tin để cập nhật ưu đãi mới nhất.',
      isOpen: false
    },
    {
      question: 'Phí vận chuyển được tính như thế nào?',
      answer: 'Phí vận chuyển phụ thuộc vào địa chỉ giao hàng và trọng lượng đơn hàng. Miễn phí vận chuyển cho đơn hàng trên 500.000đ nội thành.',
      isOpen: false
    },
    {
      question: 'Có thể thanh toán trực tuyến không?',
      answer: 'Có, chúng tôi chấp nhận thanh toán qua thẻ tín dụng, thẻ ATM, ví điện tử (MoMo, ZaloPay) và chuyển khoản ngân hàng. Tất cả giao dịch đều được bảo mật.',
      isOpen: false
    }
  ];

  supportFaqs: FaqItem[] = [
    {
      question: 'Làm sao để liên hệ bộ phận hỗ trợ?',
      answer: 'Bạn có thể liên hệ qua hotline: 1900-xxxx, email: support@healthcare.vn hoặc chat trực tuyến trên website. Chúng tôi hỗ trợ 24/7.',
      isOpen: false
    },
    {
      question: 'Tôi có thể theo dõi đơn hàng như thế nào?',
      answer: 'Sau khi đặt hàng thành công, bạn sẽ nhận được mã theo dõi qua email/SMS. Đăng nhập vào tài khoản để xem chi tiết trạng thái đơn hàng.',
      isOpen: false
    },
    {
      question: 'Làm gì khi nhận sản phẩm bị lỗi?',
      answer: 'Vui lòng liên hệ ngay bộ phận hỗ trợ trong vòng 24h kể từ khi nhận hàng. Chúng tôi sẽ hỗ trợ đổi trả hoặc hoàn tiền nhanh chóng.',
      isOpen: false
    }
  ];

  constructor(
    private productService: ProductService,
    private toastr: ToastrService,
    private authService: AuthService,
    private router: Router,
    private cartService: CartService
  ) {}

  ngOnInit(): void {
    // Initialize AOS
    AOS.init({
      duration: 1000,
      easing: 'ease-in-out',
      once: true,
      mirror: false
    });

    // Redirect admin to dashboard
    if (this.authService.isAuthenticated() && this.authService.isAdmin()) {
      this.router.navigate(['/admin/dashboard']);
      return;
    }

    // ✅ REMOVED: Load categories and products
    // this.loadCategories();
    
    // ✅ KEEP: Load testimonials and FAQ
    this.updateDisplayedTestimonials();
    this.startTestimonialSlider();
  }

  ngOnDestroy(): void {
    if (this.testimonialInterval) {
      clearInterval(this.testimonialInterval);
    }
  }

  // ===== CATEGORY & PRODUCTS METHODS ===== (Keep for future use, but not called)
  loadCategories(): void {
    console.log('🔄 Starting to load categories...');
    
    this.productService.getCategories().subscribe({
      next: (data: Category[]) => {
        console.log('✅ Loaded all categories:', data);
        this.allCategories = data;
        this.categories = this.allCategories.slice(0, 5);
        
        if (this.categories.length > 0) {
          this.filterByCategory(this.categories[0].id);
        } else {
          this.isLoading = false;
          this.toastr.warning('Không có danh mục sản phẩm nào.');
        }
      },
      error: (err) => {
        console.error('❌ Lỗi tải categories:', err);
        this.isLoading = false;
        this.toastr.error('Không thể tải danh mục sản phẩm.');
      }
    });
  }

  filterByCategory(categoryId: string): void {
    this.selectedCategory = categoryId;
    this.isLoading = true;

    const searchBody = {
      search: '',
      category: categoryId || null,
      sort: 'productName,asc',
      page: 0,
      size: 8
    };

    this.productService.searchProducts(searchBody).subscribe({
      next: (response: ProductSearchResponse) => {
        this.displayedProducts = response.content || [];
        this.isLoading = false;
        
        if (this.displayedProducts.length === 0) {
          this.toastr.info('Không có sản phẩm nào trong danh mục này');
        }

        setTimeout(() => AOS.refresh(), 100);
      },
      error: (err) => {
        console.error('❌ Lỗi tải sản phẩm:', err);
        this.isLoading = false;
        this.displayedProducts = [];
        this.toastr.error('Không thể tải danh sách sản phẩm.');
      }
    });
  }

  addToCart(product: Product): void {
    console.log('🛒 Thêm vào giỏ hàng:', product);
    
    this.cartService.addToCart(product);
    this.toastr.success(`Đã thêm "${product.productName}" vào giỏ hàng!`, 'Thành công', {
      timeOut: 2000,
      progressBar: true,
      closeButton: true
    });
  }

  scrollToProducts(): void {
    const productsSection = document.getElementById('products');
    
    if (productsSection) {
      productsSection.scrollIntoView({ 
        behavior: 'smooth',
        block: 'start'
      });
    }
  }

  // ===== TESTIMONIALS METHODS =====
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
    const testimonialsToShow: Testimonial[] = [];
    
    for (let i = 0; i < 3; i++) {
      const index = (this.currentTestimonialIndex + i) % this.allTestimonials.length;
      testimonialsToShow.push(this.allTestimonials[index]);
    }
    
    this.displayedTestimonials = testimonialsToShow;
  }

  goToTestimonial(index: number): void {
    this.currentTestimonialIndex = index;
    this.updateDisplayedTestimonials();
    this.startTestimonialSlider();
  }

  // ===== CONTACT FORM METHODS =====
  onSubmitContact(form: any): void {
    if (!form.valid) {
      this.toastr.error('Vui lòng điền đầy đủ thông tin!', 'Lỗi');
      return;
    }

    this.isSubmittingContact = true;
    console.log('📧 Sending contact message:', this.contactData);
    
    setTimeout(() => {
      this.toastr.success('Tin nhắn của bạn đã được gửi thành công!', 'Thành công', {
        timeOut: 3000,
        progressBar: true,
        closeButton: true
      });

      this.contactData = {
        name: '',
        email: '',
        subject: '',
        message: ''
      };
      form.resetForm();

      this.isSubmittingContact = false;
    }, 2000);
  }

  // ===== FAQ METHODS =====
  changeFaqTab(tab: 'general' | 'pricing' | 'support'): void {
    this.activeFaqTab = tab;
    console.log('FAQ tab changed to:', tab);
    setTimeout(() => AOS.refresh(), 100);
  }

  toggleFaq(category: 'general' | 'pricing' | 'support', index: number): void {
    let faqArray: FaqItem[];
    
    switch(category) {
      case 'general':
        faqArray = this.generalFaqs;
        break;
      case 'pricing':
        faqArray = this.pricingFaqs;
        break;
      case 'support':
        faqArray = this.supportFaqs;
        break;
    }

    faqArray[index].isOpen = !faqArray[index].isOpen;
  }

  scrollToContact(): void {
    const contactSection = document.getElementById('contact');
    
    if (contactSection) {
      contactSection.scrollIntoView({ 
        behavior: 'smooth',
        block: 'start'
      });
    }
  }
}