import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ForecastDataPoint {
  ds: string;
  yhat: number;
  yhat_lower: number;
  yhat_upper: number;
  trend: number;
}

export interface BatchForecastResult {
  success: boolean;
  product_name: string;
  forecast?: ForecastDataPoint[];
  data_points?: number;
  error?: string;
}

export interface BatchForecastResponse {
  [productId: string]: BatchForecastResult;
}

export interface BranchProduct {
  product_id: string;
  product_name: string;
  quantity: number;
}

export interface BatchForecastRequest {
  branch_id: string;
  product_ids: string[];
  history_days?: number;
}

@Injectable({
  providedIn: 'root'
})
export class ForecastService {
  private apiUrl = 'http://localhost:5000/api/forecast';

  constructor(private http: HttpClient) {}

  // Single product forecast (existing)
  forecastMedicine(
    branchId: string, 
    productId: string, 
    historyDays: number = 730
  ): Observable<ForecastDataPoint[]> {
    return this.http.get<ForecastDataPoint[]>(
      `${this.apiUrl}/medicine/${branchId}/${productId}?history_days=${historyDays}`
    );
  }

  forecastSupply(
    branchId: string, 
    productId: string, 
    historyDays: number = 730
  ): Observable<ForecastDataPoint[]> {
    return this.http.get<ForecastDataPoint[]>(
      `${this.apiUrl}/supply/${branchId}/${productId}?history_days=${historyDays}`
    );
  }

  // ✅ NEW: Batch forecast for multiple products
  forecastMedicineBatch(
    branchId: string,
    productIds: string[],
    historyDays: number = 730
  ): Observable<BatchForecastResponse> {
    const body: BatchForecastRequest = {
      branch_id: branchId,
      product_ids: productIds,
      history_days: historyDays
    };

    console.log('📊 Batch forecast request:', body);

    return this.http.post<BatchForecastResponse>(
      `${this.apiUrl}/medicine/batch`,
      body
    );
  }

  forecastSupplyBatch(
    productIds: string[],
    historyDays: number = 730
  ): Observable<BatchForecastResponse> {
    const body: BatchForecastRequest = {
      branch_id: '', // Supply không cần branch_id
      product_ids: productIds,
      history_days: historyDays
    };

    return this.http.post<BatchForecastResponse>(
      `${this.apiUrl}/supply/batch`,
      body
    );
  }

  // ✅ NEW: Get all products of branch
  getBranchProducts(branchId: string): Observable<BranchProduct[]> {
    return this.http.get<BranchProduct[]>(
      `${this.apiUrl}/branch/${branchId}/products`
    );
  }

  // ✅ Helper: Calculate suggested order quantity
  calculateSuggestedOrder(
    currentStock: number,
    forecast7Days: ForecastDataPoint[],
    safetyStock: number = 10
  ): number {
    const totalForecast = forecast7Days.reduce((sum, day) => sum + day.yhat, 0);
    const avgDaily = totalForecast / 7;
    const needed = Math.ceil(avgDaily * 14); // 2 weeks buffer
    const shouldOrder = Math.max(0, needed + safetyStock - currentStock);
    return shouldOrder;
  }
}