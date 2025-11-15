import { Injectable, inject } from '@angular/core'; // Cần Injectable
import { 
  CanActivateFn, 
  Router, 
  ActivatedRouteSnapshot, 
  RouterStateSnapshot,
  CanActivate // Cần CanActivate
} from '@angular/router';
import { AuthService } from '../services/auth'; // <-- Đảm bảo đường dẫn này đúng
import { Observable } from 'rxjs'; // Cần Observable

@Injectable({
  providedIn: 'root'
})
export class DoctorGuard implements CanActivate { // Implement CanActivate

  // Sử dụng constructor injection
  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): boolean | Observable<boolean> | Promise<boolean> {
    
    // Kiểm tra xem đã đăng nhập VÀ là bác sĩ không
    if (this.authService.isAuthenticated() && this.authService.isDoctor()) {
      return true; // OK, cho phép truy cập
    } else {
      // Chưa đăng nhập hoặc không phải bác sĩ, điều hướng về trang chủ
      console.warn('Truy cập bị từ chối, yêu cầu quyền bác sĩ!');
      this.router.navigate(['/']);
      return false; // Chặn truy cập
    }
  }
}