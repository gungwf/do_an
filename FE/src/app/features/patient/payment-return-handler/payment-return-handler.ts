import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { AppointmentService } from '../../../core/services/AppointmentService'; // Đảm bảo import đúng service
import { ToastrService } from 'ngx-toastr';
import { switchMap } from 'rxjs/operators';

@Component({
  selector: 'app-payment-return-handler',
  standalone: true,
  imports: [CommonModule],
  // Template hiển thị spinner "Đang xử lý"
  template: `
    <div class="container py-5 text-center">
      <div class="row justify-content-center">
        <div class="col-md-8">
          <div class="card shadow-sm">
            <div class="card-body p-5">
              <div class="spinner-border text-primary" role="status" style="width: 3rem; height: 3rem;">
                <span class="visually-hidden">Loading...</span>
              </div>
              <h2 class="h4 fw-bold mt-4">Đang xác thực thanh toán...</h2>
              <p class="lead text-muted">
                Vui lòng không rời khỏi trang này.
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  `
})
export class PaymentReturnHandler implements OnInit {

  constructor(
    private route: ActivatedRoute, // Để đọc tham số từ URL
    private router: Router,         // Để chuyển hướng người dùng
    private appointmentService: AppointmentService, // Để gọi API confirm
    private toastr: ToastrService
  ) {}

  ngOnInit(): void {
    // 1. Lấy tất cả query params từ URL (đống tham số của VNPay)
    this.route.queryParams.pipe(
      switchMap(params => {
        console.log('PaymentReturnHandler: Đã nhận params từ VNPay:', params);
        if (!params['vnp_TxnRef']) {
          this.toastr.error('Dữ liệu thanh toán trả về không hợp lệ.');
          throw new Error('Missing vnp_TxnRef');
        }
        
        // 2. Gọi API backend (/vnpay-return) với các params này
        this.toastr.info('Đang gửi thông tin xác thực...');
        return this.appointmentService.confirmVnPayReturn(params);
      })
    ).subscribe({
      next: (result) => {
        // 3. Backend (PaymentController) trả về JSON
        console.log('PaymentReturnHandler: Backend đã xác thực:', result);
        
        // Dựa trên file PaymentService.java, backend trả về result.status
        if (result.status === 'SUCCESS' || result.status === 'ALREADY_CONFIRMED') {
          // 4a. Nếu thành công, chuyển đến trang success (hoặc trang chủ)
          this.toastr.success(result.message || 'Thanh toán thành công!');
          // this.router.navigate(['/patient/payment-success']); // Tùy chọn 1: Trang success
          this.router.navigate(['/patient']); // Tùy chọn 2: Trang chủ patient (như bạn nói)
        } else {
          // 4b. Nếu thất bại, chuyển đến trang failed
          this.toastr.error(result.message || 'Thanh toán thất bại.');
          this.router.navigate(['/patient/payment-failed']); // Chuyển đến trang thất bại
        }
      },
      error: (err) => {
        // 4c. Nếu API /vnpay-return bị lỗi (400, 500...)
        console.error('PaymentReturnHandler: Lỗi khi gọi API confirmVnPayReturn:', err);
        this.toastr.error('Không thể xác thực thanh toán. Vui lòng liên hệ hỗ trợ.');
        this.router.navigate(['/patient/payment-failed']);
      }
    });
  }
}