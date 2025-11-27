import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { PaymentService } from '../../../core/services/payment.service';

@Component({
  selector: 'app-payment-return',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './payment-return.html'
})
export class PaymentReturn implements OnInit {
  loading = true;
  success = false;
  message = '';
  debugInfo = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private paymentService: PaymentService
  ) {}

  ngOnInit(): void {
    const queryParams = this.route.snapshot.queryParams;
    
    console.log('=== VNPAY CALLBACK ===');
    console.log('Full URL:', window.location.href);
    console.log('Query Params:', queryParams);
    
    this.debugInfo = JSON.stringify(queryParams, null, 2);
    
    const responseCode = queryParams['vnp_ResponseCode'];
    const transactionStatus = queryParams['vnp_TransactionStatus'];
    const txnRef = queryParams['vnp_TxnRef']; // VD: ORD6f9b3d8c... hoặc MR-421e6335... hoặc BILL-xxx...
    
    console.log('vnp_ResponseCode:', responseCode);
    console.log('vnp_TransactionStatus:', transactionStatus);
    console.log('vnp_TxnRef:', txnRef);
    
    if (!responseCode) {
      this.success = false;
      this.message = 'Thiếu thông tin phản hồi từ VNPay';
      this.loading = false;
      return;
    }
    
    if (responseCode === '00' && transactionStatus === '00') {
      this.processPaymentSuccess(queryParams, txnRef);
    } else {
      this.processPaymentFailure(responseCode, txnRef);
    }
  }

  processPaymentSuccess(queryParams: any, txnRef: string): void {
    console.log('Payment success, calling backend API...');
    
    // Phân biệt loại thanh toán dựa vào txnRef
    const isMedicalRecordPayment = txnRef && txnRef.startsWith('MR');
    const isAppointmentPayment = txnRef && txnRef.startsWith('ORD');
    const isBillPayment = txnRef && txnRef.startsWith('BILL');
    
    console.log('Is Medical Record Payment (MR):', isMedicalRecordPayment);
    console.log('Is Appointment Payment (ORD):', isAppointmentPayment);
    console.log('Is Bill Payment (BILL):', isBillPayment);
    
    // Gọi API tương ứng
    let apiCall;
    if (isMedicalRecordPayment) {
      apiCall = this.paymentService.processVnpayReturn(queryParams);
    } else if (isAppointmentPayment) {
      apiCall = this.paymentService.processVnpayReturnAppointment(queryParams);
    } else if (isBillPayment) {
      apiCall = this.paymentService.processVnpayReturnBill(queryParams);
    } else {
      // Mặc định xử lý như appointment
      apiCall = this.paymentService.processVnpayReturnAppointment(queryParams);
    }
    
    apiCall.subscribe({
      next: (response) => {
        console.log('✓ Backend API Response:', response);
        
        this.success = true;
        this.message = 'Thanh toán thành công!';
        this.loading = false;
        
        // Redirect tùy theo loại thanh toán
        setTimeout(() => {
          if (isMedicalRecordPayment) {
            console.log('Redirecting to /staff/medical-records (MR payment)...');
            this.router.navigate(['/staff/medical-records']);
          } else if (isBillPayment) {
            console.log('Redirecting to / (BILL payment - patient home)...');
            this.router.navigate(['/']);
          } else if (isAppointmentPayment) {
            console.log('Redirecting to / (ORD payment - patient home)...');
            this.router.navigate(['/']);
          } else {
            console.log('Unknown payment type, redirecting to home...');
            this.router.navigate(['/']);
          }
        }, 2000);
      },
      error: (error) => {
        console.error('✗ Backend API Error:', error);
        
        this.success = false;
        this.loading = false;
        
        if (error.status === 500) {
          this.message = 'Lỗi xử lý thanh toán trên server. Vui lòng liên hệ hỗ trợ.';
        } else if (error.status === 404) {
          this.message = 'Không tìm thấy API xử lý thanh toán.';
        } else if (error.error?.message) {
          this.message = error.error.message;
        } else {
          this.message = 'Có lỗi xảy ra khi xử lý thanh toán.';
        }
        
        setTimeout(() => {
          if (isMedicalRecordPayment) {
            console.log('Redirecting to /staff/medical-records (error)...');
            this.router.navigate(['/staff/medical-records']);
          } else {
            console.log('Redirecting to / (error)...');
            this.router.navigate(['/']);
          }
        }, 4000);
      }
    });
  }

  processPaymentFailure(responseCode: string, txnRef: string): void {
    console.log('Payment failed with code:', responseCode);
    
    const isMedicalRecordPayment = txnRef && txnRef.startsWith('MR');
    
    const errorMessages: { [key: string]: string } = {
      '07': 'Trừ tiền thành công nhưng giao dịch bị nghi ngờ',
      '09': 'Thẻ/Tài khoản chưa đăng ký dịch vụ Internet Banking',
      '10': 'Xác thực thông tin thẻ/tài khoản không đúng quá 3 lần',
      '11': 'Đã hết hạn chờ thanh toán',
      '12': 'Thẻ/Tài khoản bị khóa',
      '13': 'Nhập sai mật khẩu xác thực giao dịch (OTP)',
      '24': 'Khách hàng hủy giao dịch',
      '51': 'Tài khoản không đủ số dư',
      '65': 'Tài khoản đã vượt quá hạn mức giao dịch trong ngày',
      '75': 'Ngân hàng thanh toán đang bảo trì',
      '79': 'Nhập sai mật khẩu thanh toán quá số lần quy định'
    };
    
    this.success = false;
    this.message = errorMessages[responseCode] || `Thanh toán không thành công. Mã lỗi: ${responseCode}`;
    this.loading = false;
    
    setTimeout(() => {
      if (isMedicalRecordPayment) {
        console.log('Redirecting to /staff/medical-records (failure)...');
        this.router.navigate(['/staff/medical-records']);
      } else {
        console.log('Redirecting to / (failure)...');
        this.router.navigate(['/']);
      }
    }, 4000);
  }
}