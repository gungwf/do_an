import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { Branch, CreateBranchDto } from '../../features/admin/branches/branches';

@Injectable({
  providedIn: 'root'
})
export class BranchService {
  // API base URL của bạn
  private apiUrl = 'http://localhost:8080/branches';

  constructor(private http: HttpClient) { }

  /** 1. Lấy tất cả chi nhánh */
  getBranches(): Observable<Branch[]> {
    return this.http.get<Branch[]>(this.apiUrl);
  }

  /** 2. Lấy chi tiết một chi nhánh */
  getBranchById(id: string): Observable<Branch> {
    return this.http.get<Branch>(`${this.apiUrl}/${id}`);
  }

  /** 3. Thêm mới chi nhánh (chỉ 3 trường) */
  createBranch(data: CreateBranchDto): Observable<Branch> {
    // API của bạn: POST /branches với body là DTO
    return this.http.post<Branch>(this.apiUrl, data);
  }

  /** 4. Cập nhật chi nhánh (gửi full object) */
  updateBranch(branch: Branch): Observable<Branch> {
    // API của bạn: POST /branches với body là full object
    return this.http.post<Branch>(this.apiUrl, branch);
  }
}