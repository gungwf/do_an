import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { Observable, throwError, tap, catchError } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  // URL trỏ đến API Gateway của bạn
  private apiUrl = 'http://localhost:8080'; 
  private readonly TOKEN_KEY = 'healthcare_token';

  constructor(
    private router: Router,
    private http: HttpClient
  ) { }

  /**
   * Gọi API đăng ký bệnh nhân mới
   * @param userData Dữ liệu từ form đăng ký
   */
  register(userData: any): Observable<any> {
    // === THAY ĐỔI: Chỉ gửi các trường cần thiết theo yêu cầu mới ===
    const registrationData = {
      fullName: userData.fullName,
      email: userData.email,
      password: userData.password,
      phoneNumber: userData.phone, // Form dùng 'phone', backend dùng 'phoneNumber'
      role: "PATIENT" // Luôn mặc định là patient
      // branchId không cần gửi
    };
    // ==========================================================

    return this.http.post(`${this.apiUrl}/auth/register/patient`, registrationData, { responseType: 'text' });
  }

  /**
   * Gọi API đăng nhập
   * @param credentials Dữ liệu từ form đăng nhập
   */
  login(credentials: any): Observable<any> {
    const loginData = {
      email: credentials.email,
      password: credentials.password
    };

    return this.http.post<any>(`${this.apiUrl}/auth/login`, loginData).pipe(
      tap(response => {
        const token = response.token || response.accessToken;
        if (token) {
          this.saveToken(token);
        } else {
          console.error('Không tìm thấy token trong response đăng nhập:', response);
        }
      }),
      catchError(err => {
        const errorMessage = err.error?.message || err.error || 'Email hoặc mật khẩu không chính xác';
        return throwError(() => new Error(errorMessage));
      })
    );
  }

  // --- Các hàm còn lại giữ nguyên ---
  private saveToken(token: string): void {
    localStorage.setItem(this.TOKEN_KEY, token);
  }

  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  isAuthenticated(): boolean {
    return this.getToken() !== null;
  }

  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    this.router.navigate(['/']); 
  }
}

