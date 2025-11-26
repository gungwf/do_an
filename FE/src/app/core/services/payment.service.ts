import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { tap, catchError } from 'rxjs/operators';
import { throwError } from 'rxjs';

export interface GenerateBillPaymentResponse {
  payUrl: string;
}

@Injectable({ providedIn: 'root' })
export class PaymentService {
  private http = inject(HttpClient);
  private apiUrl = 'http://localhost:8080/api/v1';

  // Thanh toán đặt lịch (appointment)
  generateAppointmentPayment(appointmentId: string): Observable<GenerateBillPaymentResponse> {
    const url = `${this.apiUrl}/payment/${appointmentId}/generate-payment`;
    console.log('POST', url);
    return this.http.post<GenerateBillPaymentResponse>(url, {}).pipe(
      tap(response => console.log('✓ Appointment Payment URL:', response.payUrl)),
      catchError(error => {
        console.error('✗ Appointment Payment Error:', error);
        return throwError(() => error);
      })
    );
  }

  // Thanh toán dịch vụ (medical record) - sử dụng appointment ID
  generateBillPayment(appointmentId: string): Observable<GenerateBillPaymentResponse> {
    const url = `${this.apiUrl}/payment/${appointmentId}/generate-bill-payment`;
    console.log('POST', url);
    return this.http.post<GenerateBillPaymentResponse>(url, {}).pipe(
      tap(response => console.log('✓ Bill Payment URL:', response.payUrl)),
      catchError(error => {
        console.error('✗ Bill Payment Error:', error);
        return throwError(() => error);
      })
    );
  }

  // Xử lý return thanh toán đặt lịch (ORD)
  processVnpayReturnAppointment(queryParams: any): Observable<any> {
    let params = new HttpParams();
    Object.keys(queryParams).forEach(key => {
      if (queryParams[key]) {
        params = params.set(key, String(queryParams[key]));
      }
    });
    
    const url = `${this.apiUrl}/payment/vnpay-return`;
    console.log('GET', url, '(Appointment Payment Return)');
    console.log('Query params:', params.toString());
    
    return this.http.get(url, { params }).pipe(
      tap(response => console.log('✓ VNPay Appointment Return Success:', response)),
      catchError(error => {
        console.error('✗ VNPay Appointment Return Error:', error);
        return throwError(() => error);
      })
    );
  }

  // Xử lý return thanh toán dịch vụ (MR)
  processVnpayReturn(queryParams: any): Observable<any> {
    let params = new HttpParams();
    Object.keys(queryParams).forEach(key => {
      if (queryParams[key]) {
        params = params.set(key, String(queryParams[key]));
      }
    });
    
    const url = `${this.apiUrl}/payment/vnpay-return-medical-record`;
    console.log('GET', url, '(Medical Record Payment Return)');
    console.log('Query params:', params.toString());
    
    return this.http.get(url, { params }).pipe(
      tap(response => console.log('✓ VNPay Medical Record Return Success:', response)),
      catchError(error => {
        console.error('✗ VNPay Medical Record Return Error:', error);
        return throwError(() => error);
      })
    );
  }

  // Xử lý return thanh toán bill (BILL)
  processVnpayReturnBill(queryParams: any): Observable<any> {
    let params = new HttpParams();
    Object.keys(queryParams).forEach(key => {
      if (queryParams[key]) {
        params = params.set(key, String(queryParams[key]));
      }
    });
    
    const url = `${this.apiUrl}/payment/vnpay-return-prescription-online`;
    console.log('GET', url, '(Bill Payment Return)');
    console.log('Query params:', params.toString());
    
    return this.http.get(url, { params }).pipe(
      tap(response => console.log('✓ VNPay Bill Return Success:', response)),
      catchError(error => {
        console.error('✗ VNPay Bill Return Error:', error);
        return throwError(() => error);
      })
    );
  }
}