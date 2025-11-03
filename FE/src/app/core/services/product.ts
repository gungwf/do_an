import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

// (Optional) Định nghĩa một Interface (kiểu dữ liệu) cho Product
export interface Product {
  
  id: string;
  productName: string;
  description: string;
  price: number;
  createdAt: string;
  updatedAt: string;
  productType: string;
  category?: string | null;
  imageUrl?: string | null;
  active: boolean;
}


@Injectable({
  providedIn: 'root'
})
export class ProductService {

  private productsUrl = 'assets/mocks/products.json'; // Đường dẫn đến file mock

  constructor(private http: HttpClient) { }

  // Hàm lấy tất cả sản phẩm
  getProducts(): Observable<Product[]> {
    return this.http.get<Product[]>(this.productsUrl);
  }
  
}