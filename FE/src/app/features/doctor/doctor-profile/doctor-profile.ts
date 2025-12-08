import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AuthService, DoctorProfileDto } from '../../../core/services/auth';
import { ToastrService } from 'ngx-toastr';

@Component({
  selector: 'app-doctor-profile',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './doctor-profile.html',
  styleUrls: ['./doctor-profile.scss']
})
export class DoctorProfile implements OnInit {
  doctorProfile: DoctorProfileDto | null = null;
  isLoading: boolean = true;
  isUploadingAvatar: boolean = false;
  currentAvatarUrl: string = '';
  defaultAvatar: string = '';
  avatarLoadError: boolean = false;
  activeTab: string = 'overview';

  constructor(
    private authService: AuthService,
    private toastr: ToastrService
  ) {
    this.defaultAvatar = this.authService.getDefaultAvatar('doctor');
    this.currentAvatarUrl = this.defaultAvatar;
  }

  ngOnInit(): void {
    this.loadDoctorProfile();
  }

  loadDoctorProfile(): void {
    this.isLoading = true;

    this.authService.getCurrentDoctor().subscribe({
      next: (profile) => {
        if (profile) {
          this.doctorProfile = profile;

          if (profile.user?.avatarUrl) {
            this.currentAvatarUrl = profile.user.avatarUrl;
            this.avatarLoadError = false;
          } else {
            this.currentAvatarUrl = this.defaultAvatar;
            this.avatarLoadError = false;
          }
        } else {
          this.toastr.error('Không thể tải thông tin bác sĩ', 'Lỗi');
        }
        this.isLoading = false;
      },
      error: (error) => {
        this.toastr.error('Không thể tải thông tin bác sĩ', 'Lỗi');
        this.isLoading = false;
        this.currentAvatarUrl = this.defaultAvatar;
      }
    });
  }

  onAvatarClick(): void {
    const fileInput = document.getElementById('avatarInput') as HTMLInputElement;
    fileInput?.click();
  }

  onAvatarSelected(event: any): void {
    const file: File = event.target.files[0];
    if (!file) return;

    const validation = this.authService.validateImageFile(file);
    if (!validation.valid) {
      this.toastr.error(validation.error || 'File không hợp lệ', 'Lỗi');
      event.target.value = '';
      return;
    }

    const reader = new FileReader();
    reader.onload = (e: any) => {
      this.currentAvatarUrl = e.target.result;
      this.avatarLoadError = false;
    };
    reader.readAsDataURL(file);

    this.isUploadingAvatar = true;

    this.authService.uploadAvatar(file).subscribe({
      next: (response) => {
        this.isUploadingAvatar = false;
        
        if (response.avatarUrl) {
          this.currentAvatarUrl = response.avatarUrl;
          this.avatarLoadError = false;
          
          if (this.doctorProfile?.user) {
            this.doctorProfile.user.avatarUrl = response.avatarUrl;
          }
        }

        this.toastr.success('Cập nhật ảnh đại diện thành công!', 'Thành công');
        event.target.value = '';
      },
      error: (error) => {
        this.isUploadingAvatar = false;
        this.currentAvatarUrl = this.doctorProfile?.user?.avatarUrl || this.defaultAvatar;
        this.avatarLoadError = false;
        
        this.toastr.error(error.message || 'Không thể tải lên ảnh', 'Lỗi');
        event.target.value = '';
      }
    });
  }

  onAvatarError(event: any): void {
    if (!this.avatarLoadError) {
      this.avatarLoadError = true;
      event.target.src = this.defaultAvatar;
    }
  }

  setActiveTab(tab: string): void {
    this.activeTab = tab;
  }

  get avatarUrl(): string {
    return this.currentAvatarUrl || this.defaultAvatar;
  }

  get fullName(): string {
    return this.doctorProfile?.user?.fullName || 'Chưa có tên';
  }

  get email(): string {
    return this.doctorProfile?.user?.email || 'Chưa có email';
  }

  get phoneNumber(): string {
    return this.doctorProfile?.user?.phoneNumber || 'Chưa có SĐT';
  }

  get specialty(): string {
    return this.doctorProfile?.specialty || 'Chưa có chuyên khoa';
  }

  get degree(): string {
    return this.doctorProfile?.degree || 'Chưa có bằng cấp';
  }

  get branchName(): string {
    return this.doctorProfile?.branch?.branchName || 'Chưa có thông tin';
  }

  get branchAddress(): string {
    return this.doctorProfile?.branch?.address || 'Chưa có thông tin';
  }

  get branchPhone(): string {
    return this.doctorProfile?.branch?.phoneNumber || 'Chưa có thông tin';
  }
}