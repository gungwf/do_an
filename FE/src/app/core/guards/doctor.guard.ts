import { Injectable } from '@angular/core';
import { 
  CanActivate, 
  Router, 
  ActivatedRouteSnapshot, 
  RouterStateSnapshot
} from '@angular/router';
import { AuthService } from '../services/auth';
import { Observable, of } from 'rxjs';
import { map, catchError } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class DoctorGuard implements CanActivate {

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): Observable<boolean> | boolean {
    
    // Kiểm tra đã đăng nhập chưa
    if (!this.authService.isAuthenticated()) {
      console.warn('DoctorGuard: Chưa đăng nhập!');
      this.router.navigate(['/auth/login']);
      return false;
    }

    // Kiểm tra role từ token
    if (!this.authService.isDoctor()) {
      console.warn('DoctorGuard: Không có quyền bác sĩ!');
      this.router.navigate(['/']);
      return false;
    }

    // Nếu chưa có userId trong localStorage, gọi API lấy thông tin bác sĩ
    if (!this.authService.getUserId()) {
      console.log('DoctorGuard: Đang lấy thông tin bác sĩ...');
      return this.authService.getCurrentDoctor().pipe(
        map(doctor => {
          if (doctor && doctor.userId) {
            console.log('DoctorGuard: Đã lấy userId của bác sĩ:', doctor.userId);
            return true;
          } else {
            console.error('DoctorGuard: Không thể lấy thông tin bác sĩ!');
            this.router.navigate(['/']);
            return false;
          }
        }),
        catchError(err => {
          console.error('DoctorGuard: Lỗi khi lấy thông tin bác sĩ:', err);
          this.router.navigate(['/']);
          return of(false);
        })
      );
    }

    // Đã có userId, cho phép truy cập
    return true;
  }
}