import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

// (Optional) Định nghĩa một Interface (kiểu dữ liệu) cho Product
export interface Product {
  id: number;
  name: string;
  // description: string; // <-- Bỏ dòng này đi
  price: number;
  imageUrl: string;
  dosageForm: string; // <-- Thêm dòng này
  packaging: string;  // <-- Thêm dòng này
  category: string;
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