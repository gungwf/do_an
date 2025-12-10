import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { AuthService, PatientProfileDto, UpdatePatientProfileDto } from '../../../core/services/auth';
import { ToastrService } from 'ngx-toastr';
import * as AOS from 'aos';

@Component({
  selector: 'app-patient-profile',
  standalone: true,
  imports: [CommonModule, RouterModule, ReactiveFormsModule], // ✅ Added ReactiveFormsModule
  templateUrl: './patient-profile.html',
  styleUrls: ['./patient-profile.scss']
})
export class PatientProfile implements OnInit {
  patientProfile: PatientProfileDto | null = null;
  isLoading: boolean = true;
  
  // Avatar
  isUploadingAvatar: boolean = false;
  currentAvatarUrl: string = '';
  defaultAvatar: string = '';
  avatarLoadError: boolean = false;
  
  // Active tab
  activeTab: string = 'overview';

  // ✅ NEW: Edit Form
  editForm: FormGroup;
  isSubmitting: boolean = false;

  constructor(
    private authService: AuthService,
    private toastr: ToastrService,
    private fb: FormBuilder // ✅ Added FormBuilder
  ) {
    this.defaultAvatar = this.authService.getDefaultAvatar('patient');
    this.currentAvatarUrl = this.defaultAvatar;

    // ✅ Initialize Edit Form
    this.editForm = this.fb.group({
      dateOfBirth: ['', [Validators.required]],
      gender: ['', [Validators.required]],
      address: ['', [Validators.required, Validators.minLength(10)]],
      allergies: [''],
      contraindications: [''],
      medicalHistory: ['']
    });
  }

  ngOnInit(): void {
      AOS.init({ once: true });

    this.loadPatientProfile();
  }

  // ===== LOAD PROFILE =====
  loadPatientProfile(): void {
    this.isLoading = true;

    this.authService.getCurrentPatient().subscribe({
      next: (profile) => {
        if (profile) {
          this.patientProfile = profile;
          console.log('✅ Patient profile loaded:', profile);

          // Set avatar
          if (profile.user?.avatarUrl) {
            this.currentAvatarUrl = profile.user.avatarUrl;
            this.avatarLoadError = false;
          } else {
            this.currentAvatarUrl = this.defaultAvatar;
            this.avatarLoadError = false;
          }

          // ✅ Populate edit form with existing data
          this.populateEditForm(profile);
        } else {
          this.toastr.error('Không thể tải thông tin bệnh nhân', 'Lỗi');
        }
        this.isLoading = false;
      },
      error: (error) => {
        console.error('❌ Error loading patient profile:', error);
        this.toastr.error('Không thể tải thông tin bệnh nhân', 'Lỗi');
        this.isLoading = false;
        this.currentAvatarUrl = this.defaultAvatar;
      }
    });
  }

  // ✅ NEW: Populate form with existing data
  populateEditForm(profile: PatientProfileDto): void {
    this.editForm.patchValue({
      dateOfBirth: profile.dateOfBirth || '',
      gender: this.mapGenderToBackend(profile.gender) || '', // Convert "Nam" → "male"
      address: profile.address || '',
      allergies: profile.allergies || '',
      contraindications: profile.contraindications || '',
      medicalHistory: profile.medicalHistory || ''
    });
  }

  // ✅ NEW: Submit Edit Form
  onSubmitEditForm(): void {
    if (this.editForm.invalid) {
      this.editForm.markAllAsTouched();
      this.toastr.warning('Vui lòng điền đầy đủ thông tin bắt buộc', 'Cảnh báo');
      return;
    }

    this.isSubmitting = true;

    const formValue = this.editForm.value;
    const updateData: UpdatePatientProfileDto = {
      dateOfBirth: formValue.dateOfBirth,
      gender: formValue.gender, // Already in "male"/"female" format
      address: formValue.address,
      allergies: formValue.allergies || '',
      contraindications: formValue.contraindications || '',
      medicalHistory: formValue.medicalHistory || ''
    };

    console.log('📤 Sending update request:', updateData);

    this.authService.updatePatientProfile(updateData).subscribe({
      next: (updatedProfile) => {
        this.isSubmitting = false;
        this.patientProfile = updatedProfile;
        console.log('✅ Profile updated successfully:', updatedProfile);
        
        this.toastr.success('Cập nhật thông tin thành công!', 'Thành công');
        this.setActiveTab('overview'); // Switch to overview tab
      },
      error: (error) => {
        this.isSubmitting = false;
        console.error('❌ Error updating profile:', error);
        this.toastr.error(
          error.error?.message || 'Không thể cập nhật thông tin',
          'Lỗi'
        );
      }
    });
  }

  // ✅ NEW: Map gender display to backend format
  mapGenderToBackend(displayGender: string | null | undefined): string {
    if (!displayGender) return '';
    
    const genderMap: Record<string, string> = {
      'Nam': 'male',
      'Nữ': 'female',
      'male': 'male',
      'female': 'female'
    };
    
    return genderMap[displayGender] || '';
  }

  // ✅ NEW: Map backend gender to display format
  mapGenderToDisplay(backendGender: string | null | undefined): string {
    if (!backendGender) return 'Chưa cập nhật';
    
    const genderMap: Record<string, string> = {
      'male': 'Nam',
      'female': 'Nữ'
    };
    
    return genderMap[backendGender.toLowerCase()] || backendGender;
  }

  // ===== AVATAR UPLOAD =====
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
          
          if (this.patientProfile?.user) {
            this.patientProfile.user.avatarUrl = response.avatarUrl;
          }
        }

        this.toastr.success('Cập nhật ảnh đại diện thành công!', 'Thành công');
        event.target.value = '';
      },
      error: (error) => {
        this.isUploadingAvatar = false;
        this.currentAvatarUrl = this.patientProfile?.user?.avatarUrl || this.defaultAvatar;
        this.avatarLoadError = false;
        
        this.toastr.error(error.message || 'Không thể tải lên ảnh', 'Lỗi');
        event.target.value = '';
      }
    });
  }

  onAvatarError(event: any): void {
    if (!this.avatarLoadError) {
      console.warn('⚠️ Avatar failed to load, using default');
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
    return this.patientProfile?.user?.fullName || 'Chưa có tên';
  }

  get email(): string {
    return this.patientProfile?.user?.email || 'Chưa có email';
  }

  get phoneNumber(): string {
    return this.patientProfile?.user?.phoneNumber || 'Chưa có SĐT';
  }

  get dateOfBirth(): string {
    return this.patientProfile?.dateOfBirth || 'Chưa cập nhật';
  }

  get gender(): string {
    return this.mapGenderToDisplay(this.patientProfile?.gender ?? null);
  }

  get address(): string {
    return this.patientProfile?.address || 'Chưa cập nhật';
  }

  get allergies(): string {
    return this.patientProfile?.allergies || 'Không có';
  }

  get contraindications(): string {
    return this.patientProfile?.contraindications || 'Không có';
  }

  get medicalHistory(): string {
    return this.patientProfile?.medicalHistory || 'Chưa có lịch sử bệnh án';
  }

  get membershipTier(): string {
    const tier = this.patientProfile?.membershipTier || 'STANDARD';
    const tierMap: Record<string, string> = {
      'STANDARD': 'Thành viên Tiêu chuẩn',
      'SILVER': 'Thành viên Bạc',
      'GOLD': 'Thành viên Vàng',
      'PLATINUM': 'Thành viên Bạch kim'
    };
    return tierMap[tier] || tier;
  }

  get points(): number {
    return this.patientProfile?.points || 0;
  }

  get membershipBadgeClass(): string {
    const tier = this.patientProfile?.membershipTier || 'STANDARD';
    const badgeMap: Record<string, string> = {
      'STANDARD': 'bg-secondary',
      'SILVER': 'bg-secondary',
      'GOLD': 'bg-warning',
      'PLATINUM': 'bg-primary'
    };
    return badgeMap[tier] || 'bg-secondary';
  }

  get role(): string {
    return this.patientProfile?.user?.role?.toUpperCase() || 'PATIENT';
  }
}