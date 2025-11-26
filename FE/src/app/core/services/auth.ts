import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { Observable, throwError, of } from 'rxjs';
import { tap, catchError, map } from 'rxjs/operators';

/**
 * Data Transfer Object (DTO) cho thông tin người dùng
 * (Đây là thông tin chúng ta mong đợi nhận về từ API "Get My Profile")
 */
export interface UserDto {
  id: string;
  fullName: string;
  email: string;
  phoneNumber?: string;
  // Thêm các trường khác nếu API "Get My Profile" của bạn trả về
}
export interface DoctorDto {
  userId: string;
  specialty: string;
  degree: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  // === ĐÃ SỬA: Trỏ đến API Gateway 8080 ===
  private apiUrl = 'http://localhost:8080'; 
  
  // Key để lưu token (từ Swagger)
  private readonly TOKEN_KEY = 'healthcare_token'; 
  // Key MỚI để lưu ID người dùng
  private readonly USER_ID_KEY = 'healthcare_user_id'; 

  constructor(
    private router: Router,
    private http: HttpClient
  ) { }

  /**
   * Gọi API đăng ký bệnh nhân mới
   * @param userData Dữ liệu từ form đăng ký
   */
  register(userData: any): Observable<any> {
    const registrationData = {
      fullName: userData.fullName,
      email: userData.email,
      password: userData.password,
      phoneNumber: userData.phone,
      role: "PATIENT"
    };
    // Sửa apiUrl
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

    // Sửa apiUrl và logic
    return this.http.post<any>(`${this.apiUrl}/auth/login`, loginData).pipe(
      tap(response => {
        // Log này để bạn kiểm tra
        console.log('AuthService: Đã nhận response từ login:', response);

        // Dựa trên Swagger, chúng ta CHỈ tìm accessToken
        const token = response.accessToken; 

        if (token) {
          this.saveToken(token);
          console.log('AuthService: Đã lưu token thành công.');
          
          // QUAN TRỌNG: Chúng ta KHÔNG lưu userId ở đây nữa,
          // vì API login không trả về nó.
          
        } else {
          console.error('AuthService: Không tìm thấy accessToken trong response đăng nhập:', response);
        }
      }),
      catchError(err => {
        const errorMessage = err.error?.message || err.error || 'Email hoặc mật khẩu không chính xác';
        return throwError(() => new Error(errorMessage));
      })
    );
  }

  /**
   * LẤY THÔNG TIN NGƯỜI DÙNG HIỆN TẠI (ĐÃ SỬA LOGIC)
   * Hàm này sẽ gọi API "Get My Profile" (dùng token)
   */
  getCurrentUser(): Observable<UserDto | null> {

    // BƯỚC 1: Đã cập nhật API "Get My Profile" từ Swagger
    // Gateway (8080) sẽ tự động điều hướng /patient-profiles/me đến service (8081)
    const profileApiUrl = '/patient-profiles/me'; // <-- API ĐÚNG

    // GỌI API (Interceptor sẽ tự đính kèm token)
    // API trả về: { userId: "...", user: { id: "...", fullName: "..." }, ... }
    // Chúng ta cần trích xuất (map) object 'user' bên trong.
    return this.http.get<any>(`${this.apiUrl}${profileApiUrl}`).pipe(
      map(response => {
        if (response && response.user) {
          return response.user as UserDto; // Trả về object 'user' lồng nhau
        }
        console.warn('AuthService: API /patient-profiles/me không trả về "user" object.');
        return null; // Không tìm thấy 'user'
      }),
      tap(user => {
        // Khi lấy được thông tin user, LƯU LẠI ID
        // để dùng cho các tác vụ khác (như đặt lịch)
        if (user && user.id) {
          this.saveUserId(user.id); // Lưu ID từ object 'user'
          console.log('AuthService: Đã lấy và lưu thông tin user:', user);
        }
      }),
      catchError(err => {
        console.error('getCurrentUser: Không thể lấy thông tin user. Lỗi:', err);
        return of(null); // Trả về null nếu có lỗi
      })
    );
  }
  getCurrentDoctor(): Observable<DoctorDto | null> {
    const profileApiUrl = '/doctor-profiles/me';

    // GỌI API (Interceptor sẽ tự đính kèm token)
    return this.http.get<DoctorDto>(`${this.apiUrl}${profileApiUrl}`).pipe(
      tap(doctor => {
        // Khi lấy được thông tin doctor, LƯU LẠI userId
        if (doctor && doctor.userId) {
          this.saveUserId(doctor.userId);
          console.log('AuthService: Đã lấy và lưu thông tin bác sĩ:', doctor);
        }
      }),
      catchError(err => {
        console.error('getCurrentDoctor: Không thể lấy thông tin bác sĩ. Lỗi:', err);
        return of(null);
      })
    );
  }

  // --- CÁC HÀM TIỆN ÍCH (HELPER) ---

  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.USER_ID_KEY); // Xóa cả userId
    this.router.navigate(['/']); 
  }

  isAuthenticated(): boolean {
    return this.getToken() !== null;
  }

  // Lưu token
  private saveToken(token: string): void {
    localStorage.setItem(this.TOKEN_KEY, token);
  }

  // Lấy token (Dùng bởi Interceptor)
  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  // Lưu userId
  private saveUserId(userId: string): void {
    localStorage.setItem(this.USER_ID_KEY, userId);
  }

  // Lấy userId (Dùng để đặt lịch)
  getUserId(): string | null {
    return localStorage.getItem(this.USER_ID_KEY);
  }

  /**
   * Decode JWT token và trả về payload
   */
  private decodeToken(token: string): any {
    try {
      const base64Url = token.split('.')[1];
      const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
      const jsonPayload = decodeURIComponent(
        atob(base64)
          .split('')
          .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
          .join('')
      );
      return JSON.parse(jsonPayload);
    } catch (error) {
      console.error('Lỗi decode token:', error);
      return null;
    }
  }

  /**
   * Lấy role từ token (authorities)
   */
  getUserRole(): string | null {
    const token = this.getToken();
    if (!token) {
      return null;
    }

    const decoded = this.decodeToken(token);
    if (!decoded || !decoded.authorities) {
      return null;
    }

    // authorities là một mảng, lấy role đầu tiên
    const authorities = decoded.authorities;
    if (Array.isArray(authorities) && authorities.length > 0) {
      return authorities[0].toLowerCase();
    }

    return null;
  }

  /**
   * Kiểm tra xem user có phải admin không
   */
  isAdmin(): boolean {
    const role = this.getUserRole();
    return role === 'admin' || role === 'role_admin';
  }

  // --- BẮT ĐẦU PHẦN THÊM MỚI ---

  /**
   * Kiểm tra xem user có phải bác sĩ không
   */
  isDoctor(): boolean {
    const role = this.getUserRole();
    // Dựa trên logic của 'isAdmin', chúng ta cũng kiểm tra 'doctor' và 'role_doctor'
    return role === 'doctor' || role === 'role_doctor';
  }
  isStaff(): boolean {
    const role = this.getUserRole();
    return role === 'staff' || role === 'role_staff' ;
  }
  
  // --- KẾT THÚC PHẦN THÊM MỚI ---
}