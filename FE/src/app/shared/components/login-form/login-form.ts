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
  isSubmitting: boolean = false; // ✅ Added loading state

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

    this.isSubmitting = true; // ✅ Start loading

    const credentials = this.loginForm.getRawValue();
    this.authService.login(credentials).subscribe({
      next: () => {
        this.isSubmitting = false;

        // ✅ Get role AFTER token is saved
        const role = this.authService.getUserRole();
        console.log('🔍 Login successful, role:', role);

        // ✅ Load user info to save userId in localStorage
        this.authService.getCurrentUser().subscribe({
          next: (user) => {
            console.log('👤 User info loaded:', user);
            
            // ✅ Emit success to close modal
            this.loginSuccess.emit();
            
            // ✅ Show success message
            this.toastr.success('Đăng nhập thành công!', 'Thành công');
            
            // ✅ Navigate based on role with FULL PATH
            if (this.authService.isAdmin()) {
              console.log('🔄 Redirecting to /admin/dashboard');
              this.router.navigate(['/admin/dashboard']);
            } else if (this.authService.isDoctor()) {
              console.log('🔄 Redirecting to /doctor/dashboard');
              this.router.navigate(['/doctor/dashboard']);
            } else if (this.authService.isStaff()) {
              console.log('🔄 Redirecting to /staff/medical-records');
              this.router.navigate(['/staff/medical-records']);
            } else {
              console.log('🔄 Redirecting to home');
              this.router.navigate(['/']);
            }
          },
          error: (err) => {
            console.error('❌ Error loading user info:', err);
            // Still navigate even if user info fails
            if (this.authService.isAdmin()) {
              this.router.navigate(['/admin/dashboard']);
            } else if (this.authService.isDoctor()) {
              this.router.navigate(['/doctor/dashboard']);
            } else if (this.authService.isStaff()) {
              this.router.navigate(['/staff/dashboard']);
            } else {
              this.router.navigate(['/']);
            }
          }
        });
      },
      error: err => {
        this.isSubmitting = false;
        this.errorMessage = err.message || 'Đăng nhập thất bại';
        console.error('❌ Login error:', err);
      }
    });
  }
}