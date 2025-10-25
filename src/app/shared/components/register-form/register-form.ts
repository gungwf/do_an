import { Component, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormGroup, FormControl, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { ToastrService } from 'ngx-toastr';
import { AuthService } from '../../../core/services/auth';

// Validator tùy chỉnh để so sánh mật khẩu
export function passwordMismatchValidator(control: AbstractControl): ValidationErrors | null {
  const password = control.get('password');
  const confirmPassword = control.get('confirmPassword');
  return password && confirmPassword && password.value !== confirmPassword.value
    ? { passwordMismatch: true }
    : null;
}

@Component({
  selector: 'app-register-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './register-form.html',
})
export class RegisterForm {
  @Output() registerSuccess = new EventEmitter<void>();
  @Output() showLogin = new EventEmitter<void>();

  private vietnamesePhonePattern = '^(0[3|5|7|8|9])+([0-9]{8})$';

  // === THAY ĐỔI: Xóa dob, gender, address khỏi FormGroup ===
  registerForm = new FormGroup({
    fullName: new FormControl('', [Validators.required]),
    email: new FormControl('', [Validators.required, Validators.email]),
    phone: new FormControl('', [Validators.required, Validators.pattern(this.vietnamesePhonePattern)]),
    password: new FormControl('', [Validators.required, Validators.minLength(6)]),
    confirmPassword: new FormControl('', [Validators.required]),
  }, { validators: passwordMismatchValidator });
  // ========================================================

  constructor(
    private toastr: ToastrService,
    private authService: AuthService
  ) {}

  // Getters tiện lợi
  get fullName() { return this.registerForm.get('fullName'); }
  get email() { return this.registerForm.get('email'); }
  get phone() { return this.registerForm.get('phone'); }
  get password() { return this.registerForm.get('password'); }
  get confirmPassword() { return this.registerForm.get('confirmPassword'); }

  onSubmit() {
    if (this.registerForm.valid) {
      this.authService.register(this.registerForm.value).subscribe({
        next: () => {
          this.toastr.success('Tạo tài khoản thành công! Vui lòng đăng nhập.');
          this.registerSuccess.emit(); // Báo cho modal mẹ chuyển sang form login
        },
        error: (err) => {
          const errorMessage = err.error?.message || err.error || 'Đã có lỗi xảy ra. Vui lòng thử lại.';
          this.toastr.error(errorMessage, 'Đăng ký thất bại');
        }
      });
    } else {
      this.registerForm.markAllAsTouched();
    }
  }
}

