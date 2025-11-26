import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { BillService, CreateBillRequest } from '../../../core/services/billService';
import { CartItem, CartService } from '../../../core/services/cartService';
import { ToastrService } from 'ngx-toastr';

interface ProductShortage {
  productId: string;
  required: number;
  available: number;
}

interface ShortageErrorResponse {
  shortages: ProductShortage[];
}

@Component({
  selector: 'app-checkout-dialog',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './checkout-dialog.html',
  styleUrls: ['./checkout-dialog.scss']
})
export class CheckoutDialog {
  @Input() cartItems: CartItem[] = [];
  @Input() totalAmount: number = 0;
  @Output() closed = new EventEmitter<void>();
  
  checkoutForm: FormGroup;
  loading = false;

  constructor(
    private fb: FormBuilder,
    private billService: BillService,
    private cartService: CartService,
    private toastr: ToastrService
  ) {
    this.checkoutForm = this.fb.group({
      recipientName: ['', [Validators.required, Validators.minLength(2)]],
      recipientPhone: ['', [Validators.required, Validators.pattern(/^[0-9]{10,11}$/)]],
      recipientAddress: ['', [Validators.required, Validators.minLength(10)]]
    });
  }

  get recipientName() { return this.checkoutForm.get('recipientName'); }
  get recipientPhone() { return this.checkoutForm.get('recipientPhone'); }
  get recipientAddress() { return this.checkoutForm.get('recipientAddress'); }

  getTotalQuantity(): number {
    return this.cartItems.reduce((sum, item) => sum + item.quantity, 0);
  }

  close(): void {
    this.closed.emit();
  }

  onSubmit(): void {
    if (this.checkoutForm.invalid) {
      this.checkoutForm.markAllAsTouched();
      this.toastr.warning('Vui lòng điền đầy đủ thông tin');
      return;
    }

    if (this.cartItems.length === 0) {
      this.toastr.warning('Giỏ hàng trống');
      return;
    }

    this.loading = true;

    const request: CreateBillRequest = {
      items: this.cartItems.map(item => ({
        productId: String(item.id),
        quantity: item.quantity
      })),
      recipientName: this.checkoutForm.value.recipientName.trim(),
      recipientPhone: this.checkoutForm.value.recipientPhone.trim(),
      recipientAddress: this.checkoutForm.value.recipientAddress.trim()
    };

    // Tạo đơn hàng
    this.billService.createOnlinePurchase(request).subscribe({
      next: (billResponse) => {
        this.toastr.success('Đơn hàng đã được tạo thành công!');
        
        // Xóa giỏ hàng ngay sau khi tạo bill thành công
        this.cartService.clearCart();
        
        // Tạo link thanh toán VNPay
        this.billService.generatePayment(billResponse.billId).subscribe({
          next: (paymentResponse) => {
            this.toastr.info('Đang chuyển đến trang thanh toán...');
            this.loading = false;
            this.close();
            
            // Redirect đến VNPay
            setTimeout(() => {
              window.location.href = paymentResponse.payUrl;
            }, 300);
          },
          error: (err) => {
            this.loading = false;
            this.handlePaymentError(err, billResponse.billId);
          }
        });
      },
      error: (err) => {
        this.loading = false;
        this.handleBillError(err);
      }
    });
  }

  private handleBillError(err: any): void {
    if (err.status === 409 && err.error?.shortages) {
      this.handleShortageError(err.error as ShortageErrorResponse);
    } else if (err.status === 400) {
      this.toastr.error(err.error?.message || 'Thông tin đơn hàng không hợp lệ');
    } else if (err.status === 404) {
      this.toastr.error('Không tìm thấy sản phẩm');
    } else {
      this.toastr.error(err.error?.message || 'Không thể tạo đơn hàng. Vui lòng thử lại.');
    }
  }

  private handlePaymentError(err: any, billId: string): void {
    if (err.status === 500) {
      const errorMsg = err.error?.message || 'Lỗi hệ thống khi tạo liên kết thanh toán';
      this.toastr.error(
        `${errorMsg}. Đơn hàng đã được lưu, vui lòng liên hệ hỗ trợ.`,
        'Lỗi thanh toán',
        { timeOut: 8000 }
      );
      this.toastr.info(
        `Mã đơn hàng: ${billId.substring(0, 8)}...`,
        'Thông tin',
        { timeOut: 6000 }
      );
    } else if (err.status === 404) {
      this.toastr.error('Không tìm thấy API thanh toán. Vui lòng liên hệ hỗ trợ.');
    } else {
      this.toastr.error('Không thể tạo liên kết thanh toán. Vui lòng thử lại.');
    }
    
    setTimeout(() => this.close(), 2000);
  }

  private handleShortageError(errorResponse: ShortageErrorResponse): void {
    const shortages = errorResponse.shortages;
    
    if (!shortages || shortages.length === 0) {
      this.toastr.error('Một số sản phẩm đã hết hàng');
      return;
    }

    const shortageMessages = shortages.map(shortage => {
      const product = this.cartItems.find(item => String(item.id) === shortage.productId);
      const productName = product?.productName || 'Sản phẩm';
      return `${productName}: Yêu cầu ${shortage.required}, chỉ còn ${shortage.available}`;
    });

    const mainMessage = `Không đủ hàng:\n${shortageMessages.join('\n')}`;
    
    this.toastr.error(mainMessage, 'Thiếu hàng', {
      timeOut: 8000,
      closeButton: true,
      positionClass: 'toast-top-right',
      enableHtml: false
    });

    // Tự động cập nhật giỏ hàng
    shortages.forEach(shortage => {
      const product = this.cartItems.find(item => String(item.id) === shortage.productId);
      if (!product) return;

      if (shortage.available === 0) {
        this.cartService.removeFromCart(product.id);
      } else {
        this.cartService.updateQuantity(product.id, shortage.available);
      }
    });

    setTimeout(() => {
      this.toastr.info('Giỏ hàng đã được cập nhật');
      this.close();
    }, 1000);
  }
}