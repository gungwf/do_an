import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { tap, catchError } from 'rxjs/operators';
import { throwError } from 'rxjs';

export interface BillItem {
  productId: string;
  quantity: number;
}

export interface CreateBillRequest {
  items: BillItem[];
  recipientName: string;
  recipientPhone: string;
  recipientAddress: string;
}

export interface CreateBillResponse {
  billId: string;
  totalAmount: number;
}

export interface GeneratePaymentResponse {
  payUrl: string;
}

@Injectable({ providedIn: 'root' })
export class BillService {
  private http = inject(HttpClient);
  private apiUrl = 'http://localhost:8080';

  createOnlinePurchase(request: CreateBillRequest): Observable<CreateBillResponse> {
    const url = `${this.apiUrl}/bills/online-purchase`;
    console.log('=== CREATE BILL API ===');
    console.log('POST', url);
    console.log('Request body:', JSON.stringify(request, null, 2));
    
    return this.http.post<CreateBillResponse>(url, request).pipe(
      tap(response => {
        console.log('✓ Bill created successfully:', response);
        console.log('  - Bill ID:', response.billId);
        console.log('  - Total Amount:', response.totalAmount);
      }),
      catchError(error => {
        console.error('✗ Create bill error:', error);
        console.error('  - Status:', error.status);
        console.error('  - Error body:', error.error);
        return throwError(() => error);
      })
    );
  }

  generatePayment(billId: string): Observable<GeneratePaymentResponse> {
    const url = `${this.apiUrl}/api/v1/payment/bills/${billId}/generate-payment`;
    console.log('=== GENERATE PAYMENT API ===');
    console.log('POST', url);
    
    return this.http.post<GeneratePaymentResponse>(url, {}).pipe(
      tap(response => {
        console.log('✓ Payment URL generated:', response.payUrl);
      }),
      catchError(error => {
        console.error('✗ Generate payment error:', error);
        console.error('  - Status:', error.status);
        console.error('  - Error body:', error.error);
        return throwError(() => error);
      })
    );
  }
}