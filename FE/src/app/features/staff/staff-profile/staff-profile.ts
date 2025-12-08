import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AuthService, UserDto } from '../../../core/services/auth';
import { BranchService, Branch } from '../../../core/services/branch.service';
import { ToastrService } from 'ngx-toastr';
import { switchMap } from 'rxjs/operators';

interface StaffProfileExtended extends UserDto {
  branchId?: string;
  branchName?: string;
  position?: string; // Chức vụ
}

@Component({
  selector: 'app-staff-profile',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './staff-profile.html',
  styleUrls: ['./staff-profile.scss']
})
export class StaffProfile implements OnInit {
  // Staff profile data
  staffProfile: StaffProfileExtended | null = null;
  isLoading: boolean = true;
  
  // Avatar
  isUploadingAvatar: boolean = false;
  currentAvatarUrl: string = '';
  defaultAvatar: string = '';
  avatarLoadError: boolean = false;
  
  // Active tab
  activeTab: string = 'overview';

  constructor(
    private authService: AuthService,
    private branchService: BranchService,
    private toastr: ToastrService
  ) {
    this.defaultAvatar = this.authService.getDefaultAvatar('staff');
    this.currentAvatarUrl = this.defaultAvatar;
  }

  ngOnInit(): void {
    this.loadStaffProfile();
  }

  // ===== ✅ CẬP NHẬT: LOAD PROFILE USING NEW API =====
  loadStaffProfile(): void {
    this.isLoading = true;
    console.log('🔄 [StaffProfile] Loading profile using getUserIdFromToken()...');

    // ✅ Step 1: Get userId from token (GET /users/getId)
    this.authService.getUserIdFromToken().pipe(
      switchMap(userId => {
        console.log('✅ [StaffProfile] Got userId:', userId);
        // ✅ Step 2: Get user details by ID (GET /users/{id})
        return this.authService.getUserById(userId);
      })
    ).subscribe({
      next: (user: UserDto) => {
        console.log('✅ [StaffProfile] User details loaded:', user);
        
        this.staffProfile = user as StaffProfileExtended;

        // ✅ Set avatar URL with fallback
        if (user.avatarUrl) {
          this.currentAvatarUrl = user.avatarUrl;
          this.avatarLoadError = false;
        } else {
          this.currentAvatarUrl = this.defaultAvatar;
          this.avatarLoadError = false;
        }

        // ✅ Set default position
        if (!this.staffProfile.position) {
          this.staffProfile.position = 'Nhân viên';
        }

        // ✅ Load branch info if branchId exists
        if (this.staffProfile.branchId) {
          console.log('🔍 [StaffProfile] Loading branch info for branchId:', this.staffProfile.branchId);
          this.loadBranchInfo(this.staffProfile.branchId);
        } else {
          console.warn('⚠️ [StaffProfile] No branchId found in user profile');
          this.staffProfile.branchName = 'Chưa có thông tin chi nhánh';
        }

        this.isLoading = false;
      },
      error: (error: any) => {
        console.error('❌ [StaffProfile] Error loading profile:', error);
        this.toastr.error('Không thể tải thông tin nhân viên', 'Lỗi');
        this.isLoading = false;
        this.currentAvatarUrl = this.defaultAvatar;
      }
    });
  }

  // ✅ Load Branch Info
  loadBranchInfo(branchId: string): void {
    console.log('🔍 [StaffProfile] Loading branch info for ID:', branchId);
    
    this.branchService.getBranchById(branchId).subscribe({
      next: (branch: Branch) => {
        if (this.staffProfile) {
          this.staffProfile.branchName = branch.branchName;
          console.log('✅ [StaffProfile] Branch loaded:', branch.branchName);
        }
      },
      error: (error: any) => {
        console.error('❌ [StaffProfile] Error loading branch:', error);
        if (this.staffProfile) {
          this.staffProfile.branchName = 'Chưa xác định';
        }
      }
    });
  }

  // ===== AVATAR UPLOAD =====
  onAvatarClick(): void {
    const fileInput = document.getElementById('avatarInput') as HTMLInputElement;
    fileInput?.click();
  }

  onAvatarSelected(event: any): void {
    const file: File = event.target.files[0];
    if (!file) return;

    // Validate
    const validation = this.authService.validateImageFile(file);
    if (!validation.valid) {
      this.toastr.error(validation.error || 'File không hợp lệ', 'Lỗi');
      event.target.value = '';
      return;
    }

    // ✅ Show preview immediately
    const reader = new FileReader();
    reader.onload = (e: any) => {
      this.currentAvatarUrl = e.target.result;
      this.avatarLoadError = false;
    };
    reader.readAsDataURL(file);

    // Upload
    this.isUploadingAvatar = true;

    this.authService.uploadAvatar(file).subscribe({
      next: (response) => {
        this.isUploadingAvatar = false;
        
        if (response.avatarUrl) {
          this.currentAvatarUrl = response.avatarUrl;
          this.avatarLoadError = false;
          
          if (this.staffProfile) {
            this.staffProfile.avatarUrl = response.avatarUrl;
          }
        }

        this.toastr.success('Cập nhật ảnh đại diện thành công!', 'Thành công');
        event.target.value = '';
      },
      error: (error: any) => {
        this.isUploadingAvatar = false;
        this.currentAvatarUrl = this.staffProfile?.avatarUrl || this.defaultAvatar;
        this.avatarLoadError = false;
        
        this.toastr.error(error.message || 'Không thể tải lên ảnh', 'Lỗi');
        event.target.value = '';
      }
    });
  }

  onAvatarError(event: any): void {
    if (!this.avatarLoadError) {
      console.warn('⚠️ [StaffProfile] Avatar failed to load, using default');
      this.avatarLoadError = true;
      event.target.src = this.defaultAvatar;
    }
  }

  // ===== TAB SWITCHING =====
  setActiveTab(tab: string): void {
    this.activeTab = tab;
  }

  // ===== GETTERS =====
  get avatarUrl(): string {
    return this.currentAvatarUrl || this.defaultAvatar;
  }

  get fullName(): string {
    return this.staffProfile?.fullName || 'Chưa có tên';
  }

  get email(): string {
    return this.staffProfile?.email || 'Chưa có email';
  }

  get phoneNumber(): string {
    return this.staffProfile?.phoneNumber || 'Chưa có SĐT';
  }

  get position(): string {
    return this.staffProfile?.position || 'Nhân viên';
  }

  get branchName(): string {
    return this.staffProfile?.branchName || 'Đang tải...';
  }

  get role(): string {
    return this.staffProfile?.role || 'STAFF';
  }

  get isActive(): boolean {
    return this.staffProfile?.active !== false;
  }

  get userId(): string {
    return this.staffProfile?.id || '';
  }
}