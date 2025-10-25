import { Component, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LoginForm } from '../login-form/login-form'; // Import form con
import { RegisterForm } from '../register-form/register-form'; // Import form con

@Component({
  selector: 'app-auth-modal',
  standalone: true,
  imports: [CommonModule, LoginForm, RegisterForm], // Thêm 2 form con vào imports
  templateUrl: './auth-modal.html', // Đảm bảo tên file HTML đúng
  styleUrl: './auth-modal.scss' // Đảm bảo tên file SCSS đúng (nếu có)
})
export class AuthModal {
  @Output() closeModal = new EventEmitter<void>();
  
  // Biến để quản lý hiển thị form nào
  view: 'login' | 'register' = 'login'; 
}