import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Product {
  id: string;
  productName: string;
  description: string;
  price: number;
  
  // Các trường mới từ API Chi tiết (Optional để tương thích code cũ)
  createdAt?: string; 
  updatedAt?: string;
  productType?: string;
  category?: string; 
  active?: boolean;

  // Các trường cũ/dự phòng
  categoryName?: string; 
  categoryId?: string;
  image?: string;
  imageUrl?: string;
  stockQuantity?: number;
  quantity?: number; 
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
  private INVENTORY_URL = 'http://localhost:8080/inventory/branch';

  constructor(private http: HttpClient) {}

  /**
   * Header mặc định cho các request JSON
   */
  private getAuthHeaders(): HttpHeaders {
    const token = localStorage.getItem('access_token');
    const headersConfig: any = {
      'Content-Type': 'application/json'
    };
    
    if (token) {
      headersConfig['Authorization'] = `Bearer ${token}`;
    }
    
    return new HttpHeaders(headersConfig);
  }

  /**
   * Tìm kiếm sản phẩm phân trang (POST)
   */
  searchProducts(body: any): Observable<ProductSearchResponse> {
    return this.http.post<ProductSearchResponse>(
      `${this.BASE_URL}/search`,
      body,
      { headers: this.getAuthHeaders() }
    );
  }

  /**
   * Lấy danh sách danh mục
   */
  getCategories(): Observable<any[]> {
    return this.http.get<any[]>(
      `${this.BASE_URL}/categories`,
      { headers: this.getAuthHeaders() }
    );
  }

  /**
   * Lấy chi tiết một sản phẩm theo ID (GET)
   */
  getProductById(id: string): Observable<Product> {
    return this.http.get<Product>(
      `${this.BASE_URL}/${id}`,
      { headers: this.getAuthHeaders() }
    );
  }

  /**
   * Cập nhật thông tin sản phẩm (PUT)
   * Gửi dữ liệu dưới dạng JSON
   */
  updateProduct(id: string, product: Product): Observable<Product> {
    return this.http.put<Product>(
      `${this.BASE_URL}/${id}`,
      product,
      { headers: this.getAuthHeaders() }
    );
  }

  /**
   * Cập nhật hình ảnh sản phẩm (POST)
   * Sử dụng FormData để gửi file vật lý (Multipart), khớp với Postman
   */
  uploadProductImage(id: string, file: File): Observable<any> {
    const formData = new FormData();
    formData.append('file', file); 

    const token = localStorage.getItem('access_token');
    // Quan trọng: Không set Content-Type để trình duyệt tự điền Boundary cho Multipart
    const headers = new HttpHeaders({
      'Authorization': `Bearer ${token}`
    });

    return this.http.post(
      `${this.BASE_URL}/${id}/image`,
      formData,
      { headers: headers }
    );
  }
  createProduct(product: Product): Observable<Product> {
  return this.http.post<Product>(
    this.BASE_URL,
    product,
    { headers: this.getAuthHeaders() }
  );
  }

  /**
   * Tìm kiếm tồn kho theo chi nhánh
   */
  searchInventoryByBranch(body: {
    page: number;
    size: number;
    sortBy?: string;
    sortDir?: string;
    branchId: string;
  }): Observable<any> {
    return this.http.post<any>(
      `${this.INVENTORY_URL}/search`,
      body,
      { headers: this.getAuthHeaders() }
    );
  }
}