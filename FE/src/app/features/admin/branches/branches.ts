import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-admin-branches',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './branches.html',
  styleUrl: './branches.scss',
})
export class AdminBranches implements OnInit {
  branches: any[] = [];
  isLoading = true;

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    this.loadBranches();
  }

  loadBranches(): void {
    // TODO: Load branches from API
    this.isLoading = false;
  }

  deleteBranch(branchId: string): void {
    if (confirm('Bạn có chắc muốn xóa chi nhánh này?')) {
      // TODO: Delete branch API call
      console.log('Delete branch:', branchId);
    }
  }
}








