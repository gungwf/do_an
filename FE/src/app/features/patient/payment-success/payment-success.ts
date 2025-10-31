import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';

@Component({
  selector: 'app-payment-success',
  standalone: true,
  imports: [CommonModule],
  // Thêm template trực tiếp (inline) cho đơn giản
  template: `
    <div class="container py-5 text-center">
      <div class="row justify-content-center">
        <div class="col-md-8">
          <div class="card shadow-sm border-success">
            <div class="card-body p-5">
              <i class="bi bi-check-circle-fill text-success" style="font-size: 5rem;"></i>
              <h1 class="h2 text-success fw-bold mt-4">Thanh toán thành công!</h1>
              <p class="lead text-muted">
                Lịch hẹn của bạn đã được xác nhận.
              </p>
              <p class="mt-4">
                Bạn sẽ được tự động chuyển về trang đặt lịch hẹn sau {{ countdown }} giây...
              </p>
              <button (click)="navigateNow()" class="btn btn-primary mt-3">
                Về trang đặt lịch ngay
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  `
})
export class PaymentSuccess implements OnInit {

  countdown: number = 3;

  constructor(private router: Router) {}

  ngOnInit(): void {
    const interval = setInterval(() => {
      this.countdown--;
      if (this.countdown <= 0) {
        clearInterval(interval);
        this.navigateNow();
      }
    }, 1000);
  }

  navigateNow(): void {
    // Chuyển về trang đặt lịch
    this.router.navigate(['/patient/appointments']);
  }
}