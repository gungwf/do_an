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

    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    const credentials = this.loginForm.getRawValue();
    this.authService.login(credentials).subscribe({
      next: () => {
        // Đóng dialog trước khi navigate
        this.loginSuccess.emit();
        
        // Hiển thị thông báo
        this.toastr.success('Đăng nhập thành công!', 'Thành công');
        
        // Navigate dựa vào role
        console.log('Role:', this.authService.getUserRole());
        if (this.authService.isAdmin()) {
          this.router.navigate(['/admin']);
        } else if (this.authService.isDoctor()) {
          this.router.navigate(['/doctor']);
        } else if (this.authService.isStaff()) {
          this.router.navigate(['/staff']);
        } else {
          this.router.navigate(['/']);
        }
      },
    error: err => {
      this.errorMessage = err.message || 'Đăng nhập thất bại';
    }
  });
  }
}