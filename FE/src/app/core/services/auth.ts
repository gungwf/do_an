import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { Observable, throwError, of } from 'rxjs';
import { tap, catchError, map, switchMap } from 'rxjs/operators';
import { BranchService, Branch } from './branch.service'; // ✅ Import BranchService

// ===== DTOs =====
export interface UserDto {
  id: string;
  fullName: string;
  email: string;
  phoneNumber?: string;
  role?: string;
  avatarUrl?: string;
  branchId?: string;
  active?: boolean;
}

export interface DoctorDto {
  userId: string;
  specialty: string;
  degree: string;
}

export interface DoctorProfileResponse {
  user: UserDto;
  profile: DoctorDto;
}

export interface PatientProfileDto {
  userId: string;
  user: UserDto;
  dateOfBirth: string | null;
  gender: string | null;
  address: string | null;
  allergies: string | null;
  contraindications: string | null;
  medicalHistory: string | null;
  membershipTier: 'STANDARD' | 'SILVER' | 'GOLD' | 'PLATINUM';
  points: number;
}

export interface DoctorProfileDto extends DoctorDto {
  user?: UserDto;
  branch?: Branch; // ✅ Add branch property
}

export interface AvatarUploadResponse {
  message?: string;
  avatarUrl?: string;
  success?: boolean;
}

export interface UpdatePatientProfileDto {
  dateOfBirth: string;
  gender: 'male' | 'female';
  address: string;
  allergies: string;
  contraindications: string;
  medicalHistory: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = 'http://localhost:8080'; 
  private readonly TOKEN_KEY = 'healthcare_token'; 
  private readonly USER_ID_KEY = 'healthcare_user_id';
  private cachedRole: string | null = null;

  constructor(
    private router: Router,
    private http: HttpClient,
    private branchService: BranchService // ✅ Inject BranchService
  ) {}

  // ===== REGISTER =====
  register(userData: any): Observable<any> {
    const registrationData = {
      fullName: userData.fullName,
      email: userData.email,
      password: userData.password,
      phoneNumber: userData.phone,
      role: "PATIENT"
    };
    
    return this.http.post(`${this.apiUrl}/auth/register/patient`, registrationData, { responseType: 'text' }).pipe(
      catchError(err => throwError(() => err))
    );
  }

  // ===== LOGIN =====
  login(credentials: any): Observable<any> {
    const loginData = {
      email: credentials.email,
      password: credentials.password
    };

    return this.http.post<any>(`${this.apiUrl}/auth/login`, loginData).pipe(
      tap(response => {
        const token = response.accessToken; 
        if (token) {
          this.saveToken(token);
          this.cachedRole = null;
        }
      }),
      catchError(err => {
        const errorMessage = err.error?.message || err.error || 'Email hoặc mật khẩu không chính xác';
        return throwError(() => new Error(errorMessage));
      })
    );
  }

  // ===== GET CURRENT USER =====
  getCurrentUser(): Observable<UserDto | null> {
    const token = this.getTokenSilent();
    if (!token) return of(null);
    
    const role = this.getUserRole();
    
    if (this.isDoctor()) {
      return this.getCurrentDoctorUser();
    }
    
    if (this.isPatient()) {
      return this.getCurrentPatientUser();
    }
    
    if (this.isStaff() || this.isAdmin()) {
      return this.getUserByToken();
    }
    
    return this.getUserByToken();
  }

  // ===== GET PATIENT USER INFO =====
  private getCurrentPatientUser(): Observable<UserDto | null> {
    return this.http.get<any>(`${this.apiUrl}/patient-profiles/me`).pipe(
      tap(response => {
        if (response?.user?.id) {
          this.saveUserIdSilent(response.user.id);
        }
      }),
      map(response => response?.user || null),
      catchError(() => of(null))
    );
  }

  // ===== GET DOCTOR USER INFO =====
  private getCurrentDoctorUser(): Observable<UserDto | null> {
    return this.http.get<DoctorProfileResponse>(`${this.apiUrl}/doctor-profiles/me`).pipe(
      tap(response => {
        if (response?.user?.id) {
          this.saveUserIdSilent(response.user.id);
        }
      }),
      map(response => response?.user || null),
      catchError(() => of(null))
    );
  }

  // ===== GET USER BY TOKEN =====
  private getUserByToken(): Observable<UserDto | null> {
    return this.http.get<UserDto>(`${this.apiUrl}/users/me`).pipe(
      tap(user => {
        if (user?.id) {
          this.saveUserIdSilent(user.id);
        }
      }),
      catchError(() => of(null))
    );
  }

  // ===== GET CURRENT PATIENT (Full Profile) =====
  getCurrentPatient(): Observable<PatientProfileDto | null> {
    return this.http.get<PatientProfileDto>(`${this.apiUrl}/patient-profiles/me`).pipe(
      tap(profile => {
        if (profile?.userId) {
          this.saveUserIdSilent(profile.userId);
        }
      }),
      catchError(() => of(null))
    );
  }

  // ===== GET CURRENT DOCTOR (Full Profile with Branch) =====
  getCurrentDoctor(): Observable<DoctorProfileDto | null> {
    return this.http.get<DoctorProfileResponse>(`${this.apiUrl}/doctor-profiles/me`).pipe(
      tap(response => {
        if (response?.user?.id) {
          this.saveUserIdSilent(response.user.id);
        }
      }),
      switchMap(response => {
        if (!response?.user || !response?.profile) {
          return of(null);
        }

        const basicProfile: DoctorProfileDto = {
          userId: response.profile.userId,
          specialty: response.profile.specialty,
          degree: response.profile.degree,
          user: response.user
        };

        // ✅ Fetch branch if branchId exists
        if (response.user.branchId) {
          return this.branchService.getBranchById(response.user.branchId).pipe(
            map(branch => ({
              ...basicProfile,
              branch: branch
            })),
            catchError(() => of(basicProfile))
          );
        }

        return of(basicProfile);
      }),
      catchError(() => of(null))
    );
  }

  // ===== UPDATE PATIENT PROFILE =====
  updatePatientProfile(data: UpdatePatientProfileDto): Observable<PatientProfileDto> {
    return this.http.put<PatientProfileDto>(`${this.apiUrl}/patient-profiles/me`, data).pipe(
      catchError(err => {
        throw err;
      })
    );
  }
  
  // ===== UPLOAD AVATAR =====
  uploadAvatar(file: File): Observable<AvatarUploadResponse> {
    const formData = new FormData();
    formData.append('file', file);

    return this.http.post<AvatarUploadResponse>(
      `${this.apiUrl}/users/avatar`, 
      formData
    ).pipe(
      catchError(err => {
        const errorMessage = err.error?.message || 'Không thể tải lên ảnh đại diện';
        return throwError(() => new Error(errorMessage));
      })
    );
  }

  // ===== VALIDATE IMAGE FILE =====
  validateImageFile(file: File): { valid: boolean; error?: string } {
    const allowedTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif', 'image/webp'];
    if (!allowedTypes.includes(file.type)) {
      return {
        valid: false,
        error: 'Chỉ chấp nhận file ảnh (JPG, PNG, GIF, WEBP)'
      };
    }

    const maxSize = 5 * 1024 * 1024;
    if (file.size > maxSize) {
      return {
        valid: false,
        error: 'Kích thước file không được vượt quá 5MB'
      };
    }

    return { valid: true };
  }

  // ===== GET AVATAR URL =====
  getAvatarUrl(): Observable<string | null> {
    if (this.isPatient()) {
      return this.getCurrentUser().pipe(
        map(user => user?.avatarUrl || null)
      );
    } else if (this.isDoctor()) {
      return this.getCurrentDoctor().pipe(
        map(profile => profile?.user?.avatarUrl || null)
      );
    }
    
    return of(null);
  }

  // ===== GET DEFAULT AVATAR =====
  getDefaultAvatar(role?: string): string {
    const userRole = role || this.getUserRole();
    
    switch(userRole?.toLowerCase()) {
      case 'doctor':
      case 'role_doctor':
        return 'assets/images/default-doctor-avatar.png';
      case 'admin':
      case 'role_admin':
        return 'assets/images/default-admin-avatar.png';
      case 'staff':
      case 'role_staff':
        return 'assets/images/default-staff-avatar.png';
      default:
        return 'assets/images/default-user-avatar.png';
    }
  }

  // ===== LOGOUT =====
  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.USER_ID_KEY);
    this.cachedRole = null;
    this.router.navigate(['/']);
  }

  // ===== IS AUTHENTICATED =====
  isAuthenticated(): boolean {
    return this.getTokenSilent() !== null;
  }

  // ===== TOKEN MANAGEMENT =====
  private saveToken(token: string): void {
    localStorage.setItem(this.TOKEN_KEY, token);
  }

  private getTokenSilent(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  private saveUserIdSilent(userId: string): void {
    localStorage.setItem(this.USER_ID_KEY, userId);
  }

  getUserId(): string | null {
    return localStorage.getItem(this.USER_ID_KEY);
  }

  // ===== DECODE TOKEN =====
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
      return null;
    }
  }

  // ===== GET USER ROLE =====
  getUserRole(): string | null {
    if (this.cachedRole !== null) {
      return this.cachedRole;
    }
    
    const token = this.getTokenSilent();
    if (!token) {
      return null;
    }

    const decoded = this.decodeToken(token);
    
    if (!decoded || !decoded.authorities) {
      return null;
    }

    const authorities = decoded.authorities;
    
    if (Array.isArray(authorities) && authorities.length > 0) {
      let role = authorities[0];
      
      if (typeof role === 'string' && role.toUpperCase().startsWith('ROLE_')) {
        role = role.substring(5);
      }
      
      this.cachedRole = role.toLowerCase();
      return this.cachedRole;
    }

    return null;
  }

  // ===== ROLE CHECKS =====
  isAdmin(): boolean {
    const role = this.getUserRole();
    return role === 'admin' || role === 'role_admin';
  }

  isDoctor(): boolean {
    const role = this.getUserRole();
    return role === 'doctor' || role === 'role_doctor';
  }

  isStaff(): boolean {
    const role = this.getUserRole();
    return role === 'staff' || role === 'role_staff';
  }

  isPatient(): boolean {
    const role = this.getUserRole();
    return role === 'patient' || role === 'role_patient';
  }
}