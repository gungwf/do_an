import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-admin-appointments',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './appointments.html',
  styleUrl: './appointments.scss',
})
export class AdminAppointments implements OnInit {
  appointments: any[] = [];
  isLoading = true;

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    this.loadAppointments();
  }

  loadAppointments(): void {
    // TODO: Load appointments from API
    this.isLoading = false;
  }

  deleteAppointment(appointmentId: string): void {
    if (confirm('Bạn có chắc muốn xóa cuộc hẹn này?')) {
      // TODO: Delete appointment API call
      console.log('Delete appointment:', appointmentId);
    }
  }
}








