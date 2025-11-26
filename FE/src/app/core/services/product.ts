import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

/** Kiểu dữ liệu của một sản phẩm */
export interface Product {
  id: string;
  productName: string;
  description: string;
  price: number;
  productType: string;
  category: string;
  imageUrl: string;
  active: boolean;
}

/** Kiểu dữ liệu phản hồi khi tìm kiếm sản phẩm */
export interface ProductSearchResponse {
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  content: Product[];
  first: boolean;
  last: boolean;
  empty: boolean;
}

export interface Category {
  id: string;
  name: string;
}


@Injectable({
  providedIn: 'root'
})
export class ProductService {
  private baseUrl = 'http://localhost:8080/products'; // URL gốc của backend

  constructor(private http: HttpClient) {}

  /** Gọi API lấy danh sách danh mục sản phẩm */
  getCategories(): Observable<{ id: string; name: string }[]> {
  return this.http.get<{ id: string; name: string }[]>(`${this.baseUrl}/categories`);  }

  /** Gọi API tìm kiếm + phân trang sản phẩm */
  searchProducts(body: {
    search?: string;
    category?: string | null;
    sort?: string;
    page?: number;
    size?: number;
  }): Observable<ProductSearchResponse> {
    return this.http.post<ProductSearchResponse>(`${this.baseUrl}/search`, body);
  }
}

