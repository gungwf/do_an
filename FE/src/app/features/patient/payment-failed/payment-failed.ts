import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';

@Component({
  selector: 'app-payment-failed',
  standalone: true,
  imports: [CommonModule],
  // Thêm template trực tiếp (inline) cho đơn giản
  template: `
    <div class="container py-5 text-center">
      <div class="row justify-content-center">
        <div class="col-md-8">
          <div class="card shadow-sm border-danger">
            <div class="card-body p-5">
              <i class="bi bi-x-circle-fill text-danger" style="font-size: 5rem;"></i>
              <h1 class="h2 text-danger fw-bold mt-4">Thanh toán thất bại!</h1>
              <p class="lead text-muted">
                Đã có lỗi xảy ra trong quá trình thanh toán hoặc bạn đã hủy giao dịch.
              </p>
              <p class="mt-4">
                Lịch hẹn của bạn vẫn ở trạng thái "Chờ thanh toán". 
                Vui lòng thử lại hoặc liên hệ hỗ trợ.
              </p>
              <button (click)="navigateBack()" class="btn btn-primary mt-3">
                Thử lại
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  `
})
export class PaymentFailed {

  constructor(private router: Router) {}

  navigateBack(): void {
    // Chuyển về trang đặt lịch
    this.router.navigate(['/patient/appointments']);
  }
}