import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface InventoryItem {
  branchId: string;
  productId: string;
  productName: string;
  imageUrl?: string;
  quantity: number;
}

export interface InventorySearchRequest {
  productName?: string;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: 'ASC' | 'DESC';
  branchId?: string;
  minQuantity?: number;
  maxQuantity?: number;
  lowStockOnly?: boolean;
  productId?: string;
}

// ✅ Request body cho nhập/xuất kho
export interface StockUpdateRequest {
  productId: string;
  quantityChange: number;
}

// ✅ Response của API nhập kho
export interface StockUpdateResponse {
  code: number;
  message: string;
  result: InventoryItem;
}

export interface InventoryResponse {
  code: number;
  message: string;
  result: {
    content: InventoryItem[];
    pageable: {
      pageNumber: number;
      pageSize: number;
      sort: any[];
      offset: number;
      unpaged: boolean;
      paged: boolean;
    };
    totalElements: number;
    totalPages: number;
    size: number;
    number: number;
    first: boolean;
    last: boolean;
    numberOfElements: number;
    empty: boolean;
  };
}

@Injectable({
  providedIn: 'root'
})
export class InventoryService {
  private apiUrl = 'http://localhost:8080';

  constructor(private http: HttpClient) {}

  getInventoryByBranch(request?: InventorySearchRequest): Observable<InventoryResponse> {
    const body: InventorySearchRequest = request || {};
    return this.http.post<InventoryResponse>(
      `${this.apiUrl}/inventory/branch/search`,
      body
    );
  }

  searchByProductName(productName: string, page: number = 0, size: number = 10): Observable<InventoryResponse> {
    const body: InventorySearchRequest = {
      productName: productName,
      page: page,
      size: size,
      sortBy: 'lastUpdatedAt',
      sortDir: 'DESC'
    };
    return this.http.post<InventoryResponse>(
      `${this.apiUrl}/inventory/branch/search`,
      body
    );
  }

  // ✅ API nhập/xuất kho
  updateStock(request: StockUpdateRequest): Observable<StockUpdateResponse> {
    console.log('📦 Updating stock:', request);
    return this.http.patch<StockUpdateResponse>(
      `${this.apiUrl}/inventory/stock`,
      request
    );
  }
}