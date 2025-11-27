import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="dashboard">
      <h1 class="mb-4"><i class="bi bi-speedometer2"></i> Staff Dashboard</h1>
      
      <div class="row g-3">
        <div class="col-md-3">
          <div class="stat-card">
            <div class="stat-icon bg-primary">
              <i class="bi bi-file-medical"></i>
            </div>
            <div class="stat-content">
              <h3>0</h3>
              <p>Tổng Bệnh Án</p>
            </div>
          </div>
        </div>

        <div class="col-md-3">
          <div class="stat-card">
            <div class="stat-icon bg-success">
              <i class="bi bi-check-circle"></i>
            </div>
            <div class="stat-content">
              <h3>0</h3>
              <p>Đã Xử Lý</p>
            </div>
          </div>
        </div>

        <div class="col-md-3">
          <div class="stat-card">
            <div class="stat-icon bg-warning">
              <i class="bi bi-clock-history"></i>
            </div>
            <div class="stat-content">
              <h3>0</h3>
              <p>Chờ Xử Lý</p>
            </div>
          </div>
        </div>

        <div class="col-md-3">
          <div class="stat-card">
            <div class="stat-icon bg-info">
              <i class="bi bi-graph-up"></i>
            </div>
            <div class="stat-content">
              <h3>0%</h3>
              <p>Hiệu Suất</p>
            </div>
          </div>
        </div>
      </div>

      <div class="alert alert-info mt-4">
        <i class="bi bi-info-circle"></i> 
        Dashboard đang được phát triển. Dữ liệu sẽ được cập nhật sau khi tích hợp API.
      </div>
    </div>
  `,
  styles: [`
    .dashboard {
      h1 {
        color: #2c3e50;
        font-weight: 600;
      }
    }

    .stat-card {
      background: white;
      border-radius: 10px;
      padding: 1.5rem;
      box-shadow: 0 2px 8px rgba(0,0,0,0.1);
      display: flex;
      align-items: center;
      gap: 1rem;
      transition: transform 0.2s;

      &:hover {
        transform: translateY(-5px);
        box-shadow: 0 4px 12px rgba(0,0,0,0.15);
      }

      .stat-icon {
        width: 60px;
        height: 60px;
        border-radius: 10px;
        display: flex;
        align-items: center;
        justify-content: center;
        color: white;
        font-size: 1.75rem;
      }

      .stat-content {
        flex: 1;

        h3 {
          margin: 0;
          font-size: 2rem;
          font-weight: 700;
          color: #2c3e50;
        }

        p {
          margin: 0;
          color: #6c757d;
          font-size: 0.875rem;
        }
      }
    }
  `]
})
export class Dashboard{}