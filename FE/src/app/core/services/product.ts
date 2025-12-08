import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Product {
  id: string;
  productName: string;
  description: string;
  price: number;
  stockQuantity: number;
  image?: string; // ✅ THÊM LẠI field này
  imageUrl?: string; // ✅ Field từ backend
  categoryName?: string;
  categoryId?: string;
}

export interface ProductSearchResponse {
  content: Product[];
  totalPages: number;
  totalElements: number;
  size: number;
  number: number;
}

@Injectable({
  providedIn: 'root',
})
export class ProductService {
  private BASE_URL = 'http://localhost:8080/products';

  constructor(private http: HttpClient) {}

  private getAuthHeaders(): HttpHeaders {
    const token = localStorage.getItem('access_token');
    if (token) {
      return new HttpHeaders({
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      });
    }
    return new HttpHeaders({
      'Content-Type': 'application/json'
    });
  }

  searchProducts(body: any): Observable<ProductSearchResponse> {
    return this.http.post<ProductSearchResponse>(
      `${this.BASE_URL}/search`,
      body,
      { headers: this.getAuthHeaders() }
    );
  }

  getCategories(): Observable<any[]> {
    return this.http.get<any[]>(
      `${this.BASE_URL}/categories`,
      { headers: this.getAuthHeaders() }
    );
  }

  getProductById(id: string): Observable<Product> {
    return this.http.get<Product>(
      `${this.BASE_URL}/${id}`,
      { headers: this.getAuthHeaders() }
    );
  }
}