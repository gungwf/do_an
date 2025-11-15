import { Component, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormGroup, FormControl, Validators } from '@angular/forms';
import { AuthService } from '../../../core/services/auth';
import { ToastrService } from 'ngx-toastr';
import { Router } from '@angular/router';

@Component({
  selector: 'app-login-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './login-form.html',
})
export class LoginForm {
  @Output() loginSuccess = new EventEmitter<void>();
  @Output() showRegister = new EventEmitter<void>();

  errorMessage: string | null = null;

  loginForm = new FormGroup({
    email: new FormControl('', [Validators.required, Validators.email]),
    password: new FormControl('', [
      Validators.required,
      Validators.minLength(6),
    ]),
  });

  constructor(
    private authService: AuthService,
    private toastr: ToastrService,
    private router: Router
  ) {}

  get email() { return this.loginForm.get('email'); }
  get password() { return this.loginForm.get('password'); }

  onSubmit() {
    this.errorMessage = null;
    if (this.loginForm.valid) {
      this.authService.login(this.loginForm.getRawValue()).subscribe({
        next: (response) => {
          this.toastr.success('Đăng nhập thành công!');
          this.loginSuccess.emit(); // <-- Dòng này có thể sẽ đóng modal
  
          // --- BẮT ĐẦU THAY ĐỔI TỪ ĐÂY ---

          // Kiểm tra theo thứ tự ưu tiên: Admin -> Doctor -> User
          
          if (this.authService.isAdmin()) {
            // 1. Nếu là Admin
            this.router.navigate(['/admin/dashboard']);

          } else if (this.authService.isDoctor()) {
            // 2. (MỚI) Nếu là Bác sĩ
            // (Đường dẫn /doctor sẽ tự động chuyển đến /doctor/appointments
            //  nhờ file doctor.routes.ts)
            this.router.navigate(['/doctor']); 

          } else {
            // 3. Mặc định (Patient hoặc vai trò khác)
            // (Giữ nguyên logic cũ của bạn)
            window.location.reload(); 
          }

          // --- KẾT THÚC THAY ĐỔI ---
        },
        error: (err) => {
          this.errorMessage = err.message;
        },
      });
    } else {
      this.loginForm.markAllAsTouched();
    }
  }
}